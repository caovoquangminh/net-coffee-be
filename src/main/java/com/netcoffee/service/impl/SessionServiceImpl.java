package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.StartSessionRequest;
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
import com.netcoffee.service.SessionService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final MachineRepository machineRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final UserService userService;
    private final TransactionService transactionService;
    private final SessionMapper sessionMapper;

    @Override
    @Transactional
    public SessionResponse startSession(StartSessionRequest request) {
        TMachineEntity machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Máy không tồn tại"));

        if (machine.getStatus() != MachineStatusEnum.AVAILABLE) {
            throw new IllegalStateException("Máy " + machine.getMachineCode() + " không khả dụng");
        }

        if (sessionRepository.existsByMachineIdAndStatus(request.getMachineId(), SessionStatusEnum.ACTIVE)) {
            throw new IllegalStateException("Máy đang có session đang chạy");
        }

        // Lấy bảng giá áp dụng cho máy tại thời điểm hiện tại
        TPricingPlanEntity plan = pricingPlanRepository
                .findApplicablePlan(machine.getRoomZone(), LocalTime.now())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy bảng giá phù hợp"));

        TSessionEntity session = TSessionEntity.builder()
                .userId(request.getUserId())
                .machineId(request.getMachineId())
                .status(SessionStatusEnum.ACTIVE)
                .pricePlanId(plan.getId())
                .pricePerHourSnapshot(plan.getPricePerHour())
                .build();

        session = sessionRepository.save(session);

        // Update machine status
        machineRepository.updateStatusAndSession(machine.getId(), MachineStatusEnum.IN_USE, session.getId());

        return sessionMapper.toResponse(session);
    }

    @Override
    @Transactional
    public SessionResponse endSession(Long sessionId) {
        return doEndSession(sessionId, SessionStatusEnum.ENDED);
    }

    @Override
    @Transactional
    public SessionResponse forceEndSession(Long sessionId) {
        return doEndSession(sessionId, SessionStatusEnum.FORCE_ENDED);
    }

    private SessionResponse doEndSession(Long sessionId, SessionStatusEnum endStatus) {
        TSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại"));

        if (session.getStatus() != SessionStatusEnum.ACTIVE) {
            throw new IllegalStateException("Session không đang active");
        }

        LocalDateTime now = LocalDateTime.now();
        long durationSeconds = ChronoUnit.SECONDS.between(session.getStartedAt(), now);

        // Tính tiền: (giây / 3600) * giá/giờ
        BigDecimal hours = BigDecimal.valueOf(durationSeconds)
                .divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
        BigDecimal totalCost = hours.multiply(session.getPricePerHourSnapshot())
                .setScale(2, RoundingMode.CEILING); // làm tròn lên để không lỗ

        session.setEndedAt(now);
        session.setDurationSeconds(durationSeconds);
        session.setTotalCost(totalCost);
        session.setStatus(endStatus);
        sessionRepository.save(session);

        // Trừ tiền và ghi transaction
        transactionService.recordDeduct(
                session.getUserId(),
                totalCost,
                sessionId,
                "Phí sử dụng máy - Session #" + sessionId
        );

        // Giải phóng máy
        machineRepository.updateStatusAndSession(session.getMachineId(), MachineStatusEnum.AVAILABLE, null);

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

    /**
     * Chạy mỗi phút để check session nào hết tiền thì lock máy
     */
    @Override
    @Scheduled(fixedRate = AppConstant.SESSION_BILLING_INTERVAL_SECONDS * 1000L)
    @Transactional
    public void billingTick() {
        List<TSessionEntity> activeSessions = sessionRepository.findAllActiveSessions();
        for (TSessionEntity session : activeSessions) {
            TUserEntity user = userService.getEntityById(session.getUserId());
            if (user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                log.info("Session {} hết tiền, force end", session.getId());
                doEndSession(session.getId(), SessionStatusEnum.FORCE_ENDED);
            }
        }
    }
}
