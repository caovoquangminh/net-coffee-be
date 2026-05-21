package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.SessionStatusEnum;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.service.SessionBillingService;
import com.netcoffee.service.SessionService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionBillingServiceImpl implements SessionBillingService {

    private final SessionRepository sessionRepository;
    private final UserService userService;
    private final TransactionService transactionService;

    private SessionService sessionService;

    @Autowired
    public void setSessionService(@Lazy SessionService sessionService) {
        this.sessionService = sessionService;
    }

    // Self-injection so @Transactional(REQUIRES_NEW) on processBillingNewTx is intercepted by proxy
    @Lazy
    @Autowired
    private SessionBillingServiceImpl self;

    @Override
    @Transactional
    public void chargeMinimumFee(Long userId, Long sessionId) {
        TUserEntity user = userService.getEntityById(userId);

        if (user.getBalance().compareTo(AppConstant.SESSION_MINIMUM_CHARGE) < 0) {
            throw new IllegalStateException("Số dư không đủ để bắt đầu phiên. Cần tối thiểu "
                    + AppConstant.SESSION_MINIMUM_CHARGE.toPlainString() + "đ");
        }

        userService.deduct(userId, AppConstant.SESSION_MINIMUM_CHARGE);

        transactionService.recordDeduct(
                userId,
                AppConstant.SESSION_MINIMUM_CHARGE,
                sessionId,
                "Phí mở máy (tối thiểu " + AppConstant.SESSION_MINIMUM_MINUTES + " phút)"
        );

        log.info("Charged minimum fee: user={}, amount={}, session={}",
                userId, AppConstant.SESSION_MINIMUM_CHARGE, sessionId);
    }

    @Override
    @Scheduled(fixedRate = AppConstant.SESSION_BILLING_INTERVAL_SECONDS * 1000L)
    public void billingTick() {
        // Not transactional — each session is billed in its own independent transaction
        // so a failure on one session never rolls back billing for other sessions.
        List<TSessionEntity> activeSessions = sessionRepository.findAllActiveSessions();
        for (TSessionEntity session : activeSessions) {
            try {
                self.processBillingNewTx(session.getId());
            } catch (Exception e) {
                log.error("Billing error for session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBillingNewTx(Long sessionId) {
        TSessionEntity session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || session.getStatus() != SessionStatusEnum.ACTIVE) {
            return;
        }
        processBilling(session);
    }

    private void processBilling(TSessionEntity session) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        long elapsedMinutes = ChronoUnit.MINUTES.between(session.getStartedAt(), now);

        if (elapsedMinutes <= AppConstant.SESSION_MINIMUM_MINUTES) {
            return;
        }

        TUserEntity user = userService.getEntityById(session.getUserId());

        if (user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Session {} out of balance, force ending", session.getId());
            sessionService.forceEndSession(session.getId());
            return;
        }

        BigDecimal costPerMinute = session.getPricePerHourSnapshot()
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
        BigDecimal deductAmount = costPerMinute.min(user.getBalance());

        userService.deduct(session.getUserId(), deductAmount);

        transactionService.recordDeduct(
                session.getUserId(),
                deductAmount,
                session.getId(),
                "Phí sử dụng máy - " + elapsedMinutes + " phút"
        );

        log.debug("Billing tick: session={}, deduct={}, elapsed={}m",
                session.getId(), deductAmount, elapsedMinutes);

        TUserEntity updatedUser = userService.getEntityById(session.getUserId());
        if (updatedUser.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Session {} balance exhausted, force ending", session.getId());
            sessionService.forceEndSession(session.getId());
        }
    }
}
