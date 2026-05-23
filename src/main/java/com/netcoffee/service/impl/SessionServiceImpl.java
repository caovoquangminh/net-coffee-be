package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.StartSessionRequest;
import com.netcoffee.dto.response.ActiveSessionWithUserResponse;
import com.netcoffee.exception.InsufficientBalanceException;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.entity.TPricingPlanEntity;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.enumtype.SessionStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.mapper.SessionMapper;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.PricingPlanRepository;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.SessionBillingService;
import com.netcoffee.service.SessionService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final MachineRepository machineRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TransactionService transactionService;
    private final SessionMapper sessionMapper;

    private SessionBillingService sessionBillingService;

    @Autowired
    public void setSessionBillingService(@Lazy SessionBillingService sessionBillingService) {
        this.sessionBillingService = sessionBillingService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionResponse getOrStartSession(Long userId, Long machineId) {
        StartSessionRequest request = new StartSessionRequest();
        request.setUserId(userId);
        request.setMachineId(machineId);
        try {
            return startSession(request);
        } catch (InsufficientBalanceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not start session for user={}: {}, checking for existing", userId, e.getMessage());
            SessionResponse existing = findActiveByUserId(userId);
            if (existing != null && existing.getMachineId().equals(machineId)) {
                log.info("Reconnected to existing session: user={}, session={}", userId, existing.getId());
                return existing;
            }
            return null;
        }
    }

    @Override
    @Transactional
    public SessionResponse startSession(StartSessionRequest request) {
        // Pessimistic lock — ngăn 2 request đồng thời khởi session trên cùng 1 máy
        TMachineEntity machine = machineRepository.findByIdForUpdate(request.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Máy không tồn tại"));

        if (machine.getStatus() != MachineStatusEnum.AVAILABLE) {
            throw new IllegalStateException("Máy " + machine.getMachineCode() + " không khả dụng");
        }

        if (sessionRepository.existsByMachineIdAndStatus(request.getMachineId(), SessionStatusEnum.ACTIVE)) {
            throw new IllegalStateException("Máy đang có session đang chạy");
        }

        BigDecimal pricePerHour = pricingPlanRepository
                .findApplicablePlan(machine.getRoomZone(), LocalTime.now(ZoneOffset.UTC))
                .map(TPricingPlanEntity::getPricePerHour)
                .orElse(AppConstant.SESSION_PRICE_PER_HOUR);

        TSessionEntity session = TSessionEntity.builder()
                .userId(request.getUserId())
                .machineId(request.getMachineId())
                .status(SessionStatusEnum.ACTIVE)
                .pricePerHourSnapshot(pricePerHour)
                .build();

        session = sessionRepository.save(session);

        machineRepository.updateStatusAndSession(machine.getId(), MachineStatusEnum.IN_USE, session.getId());

        // chargeMinimumFee cũng set lastBilledAt = startedAt + SESSION_MINIMUM_MINUTES
        sessionBillingService.chargeMinimumFee(request.getUserId(), session.getId());

        log.info("Session started: user={}, machine={}, session={}, price={}",
                request.getUserId(), request.getMachineId(), session.getId(), pricePerHour);

        return sessionMapper.toResponse(session);
    }

    @Override
    @Transactional
    public SessionResponse endSession(Long sessionId, Long requestingUserId) {
        TSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại"));

        if (!session.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("Không có quyền kết thúc phiên này");
        }

        return doEndSession(session, SessionStatusEnum.ENDED);
    }

    @Override
    @Transactional
    public SessionResponse forceEndSession(Long sessionId) {
        TSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại"));

        return doEndSession(session, SessionStatusEnum.FORCE_ENDED);
    }

    private SessionResponse doEndSession(TSessionEntity session, SessionStatusEnum endStatus) {
        if (session.getStatus() != SessionStatusEnum.ACTIVE) {
            throw new IllegalStateException("Session không đang active");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Kết toán phần lẻ chưa bill trước khi đóng session
        try {
            sessionBillingService.chargeFinalBill(
                    session.getUserId(),
                    session.getId(),
                    session.getLastBilledAt(),
                    now,
                    session.getPricePerHourSnapshot()
            );
        } catch (Exception e) {
            log.warn("Final billing failed for session {}: {}", session.getId(), e.getMessage());
        }

        long durationSeconds = ChronoUnit.SECONDS.between(session.getStartedAt(), now);
        session.setEndedAt(now);
        session.setDurationSeconds(durationSeconds);
        session.setStatus(endStatus);
        sessionRepository.save(session);

        machineRepository.updateStatusAndSession(session.getMachineId(), MachineStatusEnum.AVAILABLE, null);

        log.info("Session ended: session={}, status={}, duration={}s",
                session.getId(), endStatus, durationSeconds);

        return sessionMapper.toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse findById(Long sessionId) {
        return sessionMapper.toResponse(sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> findByUserId(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(sessionMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse findActiveByUserId(Long userId) {
        return sessionRepository
                .findByUserIdAndStatus(userId, SessionStatusEnum.ACTIVE)
                .map(sessionMapper::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveSessionWithUserResponse> findAllActiveWithUserInfo() {
        List<TSessionEntity> sessions = sessionRepository.findAllActiveSessions();
        if (sessions.isEmpty()) return List.of();

        Set<Long> userIds = sessions.stream().map(TSessionEntity::getUserId).collect(Collectors.toSet());
        Map<Long, TUserEntity> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(TUserEntity::getId, u -> u));

        return sessions.stream().map(s -> {
            TUserEntity user = userMap.get(s.getUserId());
            return ActiveSessionWithUserResponse.builder()
                    .sessionId(s.getId())
                    .machineId(s.getMachineId())
                    .userId(s.getUserId())
                    .phoneNumber(user != null ? user.getPhoneNumber() : "")
                    .fullName(user != null ? user.getFullName() : null)
                    .startedAt(s.getStartedAt())
                    .build();
        }).toList();
    }
}
