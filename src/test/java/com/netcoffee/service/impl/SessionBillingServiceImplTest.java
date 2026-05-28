package com.netcoffee.service.impl;

import com.netcoffee.billing.DefaultBillingStrategy;
import com.netcoffee.constant.AppConstant;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.SessionStatusEnum;
import com.netcoffee.exception.InsufficientBalanceException;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.service.SessionService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionBillingServiceImplTest {

    @Mock SessionRepository sessionRepository;
    @Mock UserService userService;
    @Mock TransactionService transactionService;
    @Mock SessionService sessionService;
    @Mock org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    SessionBillingServiceImpl billingService;

    @BeforeEach
    void setUp() {
        billingService = new SessionBillingServiceImpl(
                sessionRepository, userService, transactionService, new DefaultBillingStrategy(), messagingTemplate);
        billingService.setSessionService(sessionService);
        billingService.self = billingService; // bypass proxy for unit tests
    }

    // =========================================================================
    // chargeMinimumFee
    // =========================================================================

    @Nested
    @DisplayName("chargeMinimumFee")
    class ChargeMinimumFeeTest {

        @Test
        @DisplayName("Happy path — deduct thành công và set lastBilledAt = startedAt + 15 phút")
        void happyPath_deductsAndSetsLastBilledAt() {
            LocalDateTime startedAt = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(10);
            TSessionEntity session = buildSession(1L, 10L, startedAt, null, SessionStatusEnum.ACTIVE);
            TUserEntity user = buildUser(10L, new BigDecimal("5000"));

            when(userService.getEntityById(10L)).thenReturn(user);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

            billingService.chargeMinimumFee(10L, 1L);

            verify(userService).deduct(10L, AppConstant.SESSION_MINIMUM_CHARGE);

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(sessionRepository).updateLastBilledAt(eq(1L), captor.capture());

            LocalDateTime expectedLastBilledAt = startedAt.plusMinutes(AppConstant.SESSION_MINIMUM_MINUTES);
            assertThat(captor.getValue()).isEqualToIgnoringNanos(expectedLastBilledAt);
        }

        @Test
        @DisplayName("Balance không đủ → InsufficientBalanceException (402), không deduct, không set lastBilledAt")
        void insufficientBalance_throwsWithoutSideEffects() {
            TUserEntity user = buildUser(10L, new BigDecimal("1000")); // < 2000
            when(userService.getEntityById(10L)).thenReturn(user);

            assertThatThrownBy(() -> billingService.chargeMinimumFee(10L, 1L))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Số dư không đủ");

            verify(userService, never()).deduct(any(), any());
            verify(sessionRepository, never()).updateLastBilledAt(any(), any());
        }
    }

    // =========================================================================
    // processBilling (core billing logic)
    // =========================================================================

    @Nested
    @DisplayName("processBilling")
    class ProcessBillingTest {

        @Test
        @DisplayName("lastBilledAt ở tương lai → chưa đến kỳ charge, bỏ qua")
        void lastBilledAtInFuture_skips() {
            LocalDateTime startedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5);
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);
            TSessionEntity session = buildSession(1L, 10L, startedAt, lastBilledAt, SessionStatusEnum.ACTIVE);

            billingService.processBilling(session);

            verify(userService, never()).getEntityById(any());
            verify(userService, never()).deduct(any(), any());
        }

        @Test
        @DisplayName("lastBilledAt = null → fallback về startedAt + 15 phút, nếu chưa qua thì bỏ qua")
        void nullLastBilledAt_usesMinimumFallback_skipsIfStillInFreePeriod() {
            // Session mới 10 phút, lastBilledAt null → free period = 15 phút chưa hết
            LocalDateTime startedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10);
            TSessionEntity session = buildSession(1L, 10L, startedAt, null, SessionStatusEnum.ACTIVE);

            billingService.processBilling(session);

            verify(userService, never()).deduct(any(), any());
        }

        @Test
        @DisplayName("Đã qua lastBilledAt 90 giây → charge đúng 200đ, cập nhật lastBilledAt")
        void ninetySecondsAfterLastBilled_chargesExactAmount() {
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(90);
            LocalDateTime startedAt = lastBilledAt.minusMinutes(15);
            TSessionEntity session = buildSession(1L, 10L, startedAt, lastBilledAt, SessionStatusEnum.ACTIVE);
            TUserEntity user = buildUser(10L, new BigDecimal("5000"));

            when(userService.getEntityById(10L)).thenReturn(user);

            billingService.processBilling(session);

            ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(userService).deduct(eq(10L), amountCaptor.capture());

            // 90 giây × (8000/3600) = 200.00đ
            assertThat(amountCaptor.getValue()).isEqualByComparingTo("200.00");

            verify(sessionRepository).updateLastBilledAt(eq(1L), any(LocalDateTime.class));
            verify(transactionService).recordDeduct(eq(10L), eq(amountCaptor.getValue()), eq(1L), contains("90s"));
        }

        @Test
        @DisplayName("Balance = 0 trước khi charge → force end session, không deduct")
        void zeroBalance_forceEndsWithoutDeduct() {
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(60);
            TSessionEntity session = buildSession(1L, 10L,
                    lastBilledAt.minusMinutes(15), lastBilledAt, SessionStatusEnum.ACTIVE);
            TUserEntity user = buildUser(10L, BigDecimal.ZERO);

            when(userService.getEntityById(10L)).thenReturn(user);

            billingService.processBilling(session);

            verify(sessionService).forceEndSession(1L);
            verify(userService, never()).deduct(any(), any());
        }

        @Test
        @DisplayName("Balance < số tiền owed → charge tối đa bằng balance còn lại")
        void balanceLessThanOwed_chargesRemainingBalance() {
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(3600);
            TSessionEntity session = buildSession(1L, 10L,
                    lastBilledAt.minusMinutes(15), lastBilledAt, SessionStatusEnum.ACTIVE);
            TUserEntity richBalance = buildUser(10L, new BigDecimal("100")); // chỉ còn 100đ
            TUserEntity exhausted = buildUser(10L, BigDecimal.ZERO);

            when(userService.getEntityById(10L))
                    .thenReturn(richBalance)  // lần 1: lấy balance trước deduct
                    .thenReturn(exhausted);   // lần 2: check sau deduct

            billingService.processBilling(session);

            ArgumentCaptor<BigDecimal> cap = ArgumentCaptor.forClass(BigDecimal.class);
            verify(userService).deduct(eq(10L), cap.capture());
            // Không được charge quá balance
            assertThat(cap.getValue()).isEqualByComparingTo("100");

            // Sau khi hết tiền → force end
            verify(sessionService).forceEndSession(1L);
        }

        @Test
        @DisplayName("Sau khi deduct balance về 0 → force end session")
        void balanceExhaustedAfterDeduct_forceEnds() {
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(60);
            TSessionEntity session = buildSession(1L, 10L,
                    lastBilledAt.minusMinutes(15), lastBilledAt, SessionStatusEnum.ACTIVE);
            TUserEntity before = buildUser(10L, new BigDecimal("133.33")); // vừa đủ 1 phút
            TUserEntity after  = buildUser(10L, BigDecimal.ZERO);

            when(userService.getEntityById(10L))
                    .thenReturn(before)
                    .thenReturn(after);

            billingService.processBilling(session);

            verify(sessionService).forceEndSession(1L);
        }
    }

    // =========================================================================
    // chargeFinalBill
    // =========================================================================

    @Nested
    @DisplayName("chargeFinalBill — kết toán cuối phiên")
    class ChargeFinalBillTest {

        @Test
        @DisplayName("90 giây chưa bill → charge đúng 200đ")
        void nintySecondsUnbilled_chargesCorrectly() {
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(90);
            LocalDateTime endedAt = LocalDateTime.now(ZoneOffset.UTC);
            TUserEntity user = buildUser(10L, new BigDecimal("5000"));

            when(userService.getEntityById(10L)).thenReturn(user);

            billingService.chargeFinalBill(10L, 1L, lastBilledAt, endedAt, new BigDecimal("8000"));

            ArgumentCaptor<BigDecimal> cap = ArgumentCaptor.forClass(BigDecimal.class);
            verify(userService).deduct(eq(10L), cap.capture());
            assertThat(cap.getValue()).isEqualByComparingTo("200.00");
            verify(transactionService).recordDeduct(eq(10L), eq(cap.getValue()), eq(1L), contains("90s"));
        }

        @Test
        @DisplayName("Session kết thúc trước lastBilledAt (trong 15 phút free) → không charge")
        void endedBeforeLastBilledAt_noCharge() {
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5);
            LocalDateTime endedAt = LocalDateTime.now(ZoneOffset.UTC);

            billingService.chargeFinalBill(10L, 1L, lastBilledAt, endedAt, new BigDecimal("8000"));

            verify(userService, never()).getEntityById(any());
            verify(userService, never()).deduct(any(), any());
        }

        @Test
        @DisplayName("lastBilledAt = null → không charge")
        void nullLastBilledAt_noCharge() {
            billingService.chargeFinalBill(10L, 1L, null,
                    LocalDateTime.now(ZoneOffset.UTC), new BigDecimal("8000"));

            verify(userService, never()).deduct(any(), any());
        }

        @Test
        @DisplayName("Balance = 0 → không charge")
        void zeroBalance_skipsCharge() {
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(60);
            TUserEntity user = buildUser(10L, BigDecimal.ZERO);

            when(userService.getEntityById(10L)).thenReturn(user);

            billingService.chargeFinalBill(10L, 1L, lastBilledAt,
                    LocalDateTime.now(ZoneOffset.UTC), new BigDecimal("8000"));

            verify(userService, never()).deduct(any(), any());
        }

        @Test
        @DisplayName("Balance < tiền owed → charge tối đa bằng balance")
        void balanceLessThanOwed_chargesRemainingBalance() {
            LocalDateTime lastBilledAt = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(3600); // 1 giờ
            LocalDateTime endedAt = LocalDateTime.now(ZoneOffset.UTC);
            TUserEntity user = buildUser(10L, new BigDecimal("500")); // còn 500đ < 8000đ

            when(userService.getEntityById(10L)).thenReturn(user);

            billingService.chargeFinalBill(10L, 1L, lastBilledAt, endedAt, new BigDecimal("8000"));

            ArgumentCaptor<BigDecimal> cap = ArgumentCaptor.forClass(BigDecimal.class);
            verify(userService).deduct(eq(10L), cap.capture());
            assertThat(cap.getValue()).isEqualByComparingTo("500");
        }

        @Test
        @DisplayName("endedAt = lastBilledAt (0 giây) → không charge")
        void sameTimestamp_noCharge() {
            LocalDateTime ts = LocalDateTime.now(ZoneOffset.UTC);

            billingService.chargeFinalBill(10L, 1L, ts, ts, new BigDecimal("8000"));

            verify(userService, never()).deduct(any(), any());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private TSessionEntity buildSession(Long id, Long userId, LocalDateTime startedAt,
                                         LocalDateTime lastBilledAt, SessionStatusEnum status) {
        return TSessionEntity.builder()
                .id(id)
                .userId(userId)
                .machineId(1L)
                .startedAt(startedAt)
                .lastBilledAt(lastBilledAt)
                .status(status)
                .pricePerHourSnapshot(AppConstant.SESSION_PRICE_PER_HOUR)
                .build();
    }

    private TUserEntity buildUser(Long id, BigDecimal balance) {
        return TUserEntity.builder()
                .id(id)
                .balance(balance)
                .isActive(true)
                .build();
    }
}
