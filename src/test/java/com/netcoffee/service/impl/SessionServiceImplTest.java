package com.netcoffee.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.enumtype.SessionStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.mapper.SessionMapper;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.PricingPlanRepository;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.SessionBillingService;
import com.netcoffee.service.TransactionService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock SessionRepository sessionRepository;
    @Mock MachineRepository machineRepository;
    @Mock PricingPlanRepository pricingPlanRepository;
    @Mock UserRepository userRepository;
    @Mock TransactionService transactionService;
    @Mock SessionMapper sessionMapper;
    @Mock SessionBillingService sessionBillingService;

    SessionServiceImpl sessionService;

    @BeforeEach
    void setUp() {
        sessionService =
                new SessionServiceImpl(
                        sessionRepository,
                        machineRepository,
                        pricingPlanRepository,
                        userRepository,
                        transactionService,
                        sessionMapper);
        sessionService.setSessionBillingService(sessionBillingService);
    }

    // =========================================================================
    // endSession
    // =========================================================================

    @Nested
    @DisplayName("endSession")
    class EndSessionTest {

        @Test
        @DisplayName("Owner kết thúc session → gọi chargeFinalBill, cập nhật status ENDED")
        void owner_callsChargeFinalBillAndEnds() {
            LocalDateTime startedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20);
            LocalDateTime lastBilledAt = startedAt.plusMinutes(15);
            TSessionEntity session = buildActiveSession(1L, 10L, startedAt, lastBilledAt);
            SessionResponse response = buildResponse(1L);

            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(any())).thenReturn(response);

            SessionResponse result = sessionService.endSession(1L, 10L);

            verify(sessionBillingService)
                    .chargeFinalBill(
                            eq(10L),
                            eq(1L),
                            eq(lastBilledAt),
                            any(LocalDateTime.class),
                            eq(AppConstant.SESSION_PRICE_PER_HOUR));
            assertThat(session.getStatus()).isEqualTo(SessionStatusEnum.ENDED);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Người khác cố kết thúc session → AccessDeniedException")
        void nonOwner_throwsAccessDenied() {
            TSessionEntity session =
                    buildActiveSession(
                            1L, 10L, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5), null);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> sessionService.endSession(1L, 99L))
                    .isInstanceOf(AccessDeniedException.class);

            verify(sessionBillingService, never())
                    .chargeFinalBill(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Session không tồn tại → ResourceNotFoundException")
        void notFound_throwsException() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.endSession(1L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Session đã ENDED → IllegalStateException")
        void alreadyEnded_throwsException() {
            TSessionEntity session =
                    TSessionEntity.builder()
                            .id(1L)
                            .userId(10L)
                            .status(SessionStatusEnum.ENDED)
                            .pricePerHourSnapshot(AppConstant.SESSION_PRICE_PER_HOUR)
                            .startedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30))
                            .build();
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> sessionService.endSession(1L, 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("không đang active");
        }
    }

    // =========================================================================
    // forceEndSession
    // =========================================================================

    @Nested
    @DisplayName("forceEndSession")
    class ForceEndSessionTest {

        @Test
        @DisplayName("Force end → gọi chargeFinalBill, cập nhật status FORCE_ENDED")
        void forceEnd_callsChargeFinalBillAndForceEnds() {
            LocalDateTime startedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20);
            LocalDateTime lastBilledAt = startedAt.plusMinutes(15);
            TSessionEntity session = buildActiveSession(1L, 10L, startedAt, lastBilledAt);
            SessionResponse response = buildResponse(1L);

            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(any())).thenReturn(response);

            sessionService.forceEndSession(1L);

            verify(sessionBillingService)
                    .chargeFinalBill(
                            eq(10L),
                            eq(1L),
                            eq(lastBilledAt),
                            any(LocalDateTime.class),
                            eq(AppConstant.SESSION_PRICE_PER_HOUR));
            assertThat(session.getStatus()).isEqualTo(SessionStatusEnum.FORCE_ENDED);
        }

        @Test
        @DisplayName("chargeFinalBill ném exception → session vẫn được kết thúc (không propagate)")
        void chargeFinalBillFails_sessionStillEnds() {
            TSessionEntity session =
                    buildActiveSession(
                            1L, 10L, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20), null);

            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(any())).thenReturn(buildResponse(1L));
            doThrow(new RuntimeException("DB error"))
                    .when(sessionBillingService)
                    .chargeFinalBill(any(), any(), any(), any(), any());

            assertThatCode(() -> sessionService.forceEndSession(1L)).doesNotThrowAnyException();
            assertThat(session.getStatus()).isEqualTo(SessionStatusEnum.FORCE_ENDED);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private TSessionEntity buildActiveSession(
            Long id, Long userId, LocalDateTime startedAt, LocalDateTime lastBilledAt) {
        return TSessionEntity.builder()
                .id(id)
                .userId(userId)
                .machineId(1L)
                .status(SessionStatusEnum.ACTIVE)
                .startedAt(startedAt)
                .lastBilledAt(lastBilledAt)
                .pricePerHourSnapshot(AppConstant.SESSION_PRICE_PER_HOUR)
                .build();
    }

    private SessionResponse buildResponse(Long id) {
        return SessionResponse.builder().id(id).build();
    }
}
