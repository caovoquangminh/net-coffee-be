package com.netcoffee.service.impl;

import com.netcoffee.billing.BillingStrategy;
import com.netcoffee.constant.AppConstant;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.SessionStatusEnum;
import com.netcoffee.exception.InsufficientBalanceException;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.service.SessionBillingService;
import com.netcoffee.service.SessionService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionBillingServiceImpl implements SessionBillingService {

    private final SessionRepository sessionRepository;
    private final UserService userService;
    private final TransactionService transactionService;
    private final BillingStrategy billingStrategy;
    private final SimpMessagingTemplate messagingTemplate;

    private SessionService sessionService;

    @Autowired
    public void setSessionService(@Lazy SessionService sessionService) {
        this.sessionService = sessionService;
    }

    // Self-injection để @Transactional(REQUIRES_NEW) trên processBillingNewTx được proxy intercept
    @Lazy
    @Autowired
    SessionBillingServiceImpl self;

    // -------------------------------------------------------------------------
    // chargeMinimumFee
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void chargeMinimumFee(Long userId, Long sessionId) {
        TUserEntity user = userService.getEntityById(userId);

        if (user.getBalance().compareTo(AppConstant.SESSION_MINIMUM_CHARGE) < 0) {
            throw new InsufficientBalanceException("Số dư không đủ để mở máy. Cần tối thiểu "
                    + AppConstant.SESSION_MINIMUM_CHARGE.toPlainString() + "đ");
        }

        userService.deduct(userId, AppConstant.SESSION_MINIMUM_CHARGE);

        transactionService.recordDeduct(
                userId,
                AppConstant.SESSION_MINIMUM_CHARGE,
                sessionId,
                "Phí mở máy (tối thiểu " + AppConstant.SESSION_MINIMUM_MINUTES + " phút)"
        );

        // Đánh dấu SESSION_MINIMUM_MINUTES đầu tiên đã được thanh toán trước
        sessionRepository.findById(sessionId).ifPresent(session -> {
            LocalDateTime billedUpTo = session.getStartedAt()
                    .plusMinutes(AppConstant.SESSION_MINIMUM_MINUTES);
            sessionRepository.updateLastBilledAt(sessionId, billedUpTo);
        });

        BigDecimal balanceAfterMinFee = user.getBalance().subtract(AppConstant.SESSION_MINIMUM_CHARGE);
        messagingTemplate.convertAndSend("/topic/balance/" + userId, Map.of("balance", balanceAfterMinFee));

        log.info("Charged minimum fee: user={}, amount={}, session={}",
                userId, AppConstant.SESSION_MINIMUM_CHARGE, sessionId);
    }

    // -------------------------------------------------------------------------
    // billingTick — mỗi session chạy trong transaction riêng (REQUIRES_NEW)
    // -------------------------------------------------------------------------

    @Override
    @Scheduled(fixedRate = AppConstant.SESSION_BILLING_INTERVAL_SECONDS * 1000L)
    public void billingTick() {
        try {
            sessionService.cleanUpStaleSessions();
        } catch (Exception e) {
            log.error("Stale session cleanup error: {}", e.getMessage());
        }

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

    // -------------------------------------------------------------------------
    // processBilling — trừ tiền theo giây thực từ lastBilledAt
    // -------------------------------------------------------------------------

    void processBilling(TSessionEntity session) {
        if (Boolean.TRUE.equals(session.getIsFree())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Điểm bắt đầu charge: sau khi phí minimum đã cover
        LocalDateTime lastBilledAt = session.getLastBilledAt() != null
                ? session.getLastBilledAt()
                : session.getStartedAt().plusMinutes(AppConstant.SESSION_MINIMUM_MINUTES);

        // Chưa qua kỳ đã trả trước → bỏ qua
        if (!now.isAfter(lastBilledAt)) {
            return;
        }

        TUserEntity user = userService.getEntityById(session.getUserId());

        if (user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Session {} out of balance, force ending", session.getId());
            sessionService.forceEndSession(session.getId());
            return;
        }

        long unbilledSeconds = ChronoUnit.SECONDS.between(lastBilledAt, now);
        BigDecimal deductAmount = billingStrategy.calcCharge(session.getPricePerHourSnapshot(), unbilledSeconds)
                .min(user.getBalance());

        userService.deduct(session.getUserId(), deductAmount);

        // Cập nhật lastBilledAt = now trước khi record transaction
        sessionRepository.updateLastBilledAt(session.getId(), now);

        transactionService.recordDeduct(
                session.getUserId(),
                deductAmount,
                session.getId(),
                "Phí sử dụng máy (" + unbilledSeconds + "s)"
        );

        BigDecimal newBalance = user.getBalance().subtract(deductAmount);
        messagingTemplate.convertAndSend("/topic/balance/" + session.getUserId(), Map.of("balance", newBalance));

        log.debug("Billing tick: session={}, unbilled={}s, deduct={}",
                session.getId(), unbilledSeconds, deductAmount);

        // deductAmount = calcCharge.min(user.getBalance()) → nếu deductAmount >= balance
        // thì toàn bộ balance vừa bị lấy hết. Không đọc lại entity (L1 cache sẽ stale).
        if (deductAmount.compareTo(user.getBalance()) >= 0) {
            log.info("Session {} balance exhausted after billing, force ending", session.getId());
            sessionService.forceEndSession(session.getId());
        }
    }

    // -------------------------------------------------------------------------
    // chargeFinalBill — kết toán phần lẻ khi session kết thúc
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void chargeFinalBill(Long userId, Long sessionId,
                                LocalDateTime lastBilledAt, LocalDateTime endedAt,
                                BigDecimal pricePerHour) {
        if (lastBilledAt == null || !endedAt.isAfter(lastBilledAt)) {
            return;
        }

        long unbilledSeconds = ChronoUnit.SECONDS.between(lastBilledAt, endedAt);
        if (unbilledSeconds <= 0) {
            return;
        }

        TUserEntity user = userService.getEntityById(userId);
        if (user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal deductAmount = billingStrategy.calcCharge(pricePerHour, unbilledSeconds)
                .min(user.getBalance());

        if (deductAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        userService.deduct(userId, deductAmount);

        transactionService.recordDeduct(
                userId,
                deductAmount,
                sessionId,
                "Kết toán cuối phiên (" + unbilledSeconds + "s)"
        );

        BigDecimal newBalance = user.getBalance().subtract(deductAmount);
        messagingTemplate.convertAndSend("/topic/balance/" + userId, Map.of("balance", newBalance));

        log.info("Final billing: session={}, unbilled={}s, deduct={}",
                sessionId, unbilledSeconds, deductAmount);
    }

}
