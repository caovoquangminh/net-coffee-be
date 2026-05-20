package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.StartSessionRequest;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.entity.TPricingPlanEntity;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.enumtype.SessionStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.mapper.SessionMapper;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.PricingPlanRepository;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.service.SessionBillingService;
import com.netcoffee.service.SessionService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
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

    // Inject qua setter để tránh circular dependency
    private SessionBillingService sessionBillingService;

    @Autowired
    public void setSessionBillingService(@Lazy SessionBillingService sessionBillingService) {
        this.sessionBillingService = sessionBillingService;
    }

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

        // Lấy bảng giá — fallback về giá mặc định nếu không có plan
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

        // Trừ phí mở máy ngay sau khi tạo session
        sessionBillingService.chargeMinimumFee(request.getUserId(), session.getId());

        log.info("Session started: user={}, machine={}, session={}, price={}",
                request.getUserId(), request.getMachineId(), session.getId(), pricePerHour);

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

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        long durationSeconds = ChronoUnit.SECONDS.between(session.getStartedAt(), now);

        session.setEndedAt(now);
        session.setDurationSeconds(durationSeconds);
        session.setStatus(endStatus);
        sessionRepository.save(session);

        machineRepository.updateStatusAndSession(session.getMachineId(), MachineStatusEnum.AVAILABLE, null);

        log.info("Session ended: session={}, status={}, duration={}s",
                sessionId, endStatus, durationSeconds);

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
}