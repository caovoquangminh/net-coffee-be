package com.netcoffee.utils;

import static org.assertj.core.api.Assertions.*;

import com.netcoffee.enumtype.AttendanceStatusEnum;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Logic tính công — bịt kẽ hở phồng giờ/lương trong chấm công. */
class AttendanceCalcTest {

    private final LocalDateTime shiftStart = LocalDateTime.of(2026, 6, 9, 7, 0);
    private final LocalDateTime shiftEnd = LocalDateTime.of(2026, 6, 9, 15, 0); // ca 8 tiếng

    @Nested
    @DisplayName("effectiveCheckOut — cap giờ tại cuối ca")
    class EffectiveCheckOut {

        @Test
        @DisplayName("Check-out trong ca → giữ nguyên")
        void withinShift_unchanged() {
            LocalDateTime out = shiftEnd.minusMinutes(30);
            assertThat(AttendanceCalc.effectiveCheckOut(out, shiftEnd, false)).isEqualTo(out);
        }

        @Test
        @DisplayName("Quên check-out, ra sau cuối ca, KHÔNG có OT → cap tại cuối ca")
        void afterShiftNoOt_cappedAtShiftEnd() {
            LocalDateTime out = shiftEnd.plusHours(5); // ở lại 5 tiếng quá giờ
            assertThat(AttendanceCalc.effectiveCheckOut(out, shiftEnd, false)).isEqualTo(shiftEnd);
        }

        @Test
        @DisplayName("Ra sau cuối ca, CÓ OT duyệt → tính tới thời điểm thực")
        void afterShiftWithOt_keepsActual() {
            LocalDateTime out = shiftEnd.plusHours(2);
            assertThat(AttendanceCalc.effectiveCheckOut(out, shiftEnd, true)).isEqualTo(out);
        }
    }

    @Nested
    @DisplayName("workedHours — tính chính xác, không over-pay")
    class WorkedHours {

        @Test
        @DisplayName("Đúng 8 tiếng → 8.00")
        void fullShift() {
            assertThat(AttendanceCalc.workedHours(shiftStart, shiftEnd))
                    .isEqualByComparingTo("8.00");
        }

        @Test
        @DisplayName("16 phút → 0.27h (KHÔNG làm tròn lên 0.5h như công thức 30 phút cũ)")
        void sixteenMinutes_noInflation() {
            LocalDateTime out = shiftStart.plusMinutes(16);
            assertThat(AttendanceCalc.workedHours(shiftStart, out)).isEqualByComparingTo("0.27");
        }

        @Test
        @DisplayName("check-out <= check-in → 0")
        void nonPositive_zero() {
            assertThat(AttendanceCalc.workedHours(shiftEnd, shiftStart)).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("null → 0 (an toàn)")
        void nullInput_zero() {
            assertThat(AttendanceCalc.workedHours(null, shiftEnd)).isEqualByComparingTo("0");
            assertThat(AttendanceCalc.workedHours(shiftStart, null)).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("resolveCheckoutStatus — phát hiện về sớm")
    class ResolveStatus {

        @Test
        @DisplayName("Về sớm hơn 1 tiếng → EARLY_LEAVE")
        void earlyLeave() {
            LocalDateTime out = shiftEnd.minusHours(1);
            assertThat(
                            AttendanceCalc.resolveCheckoutStatus(
                                    AttendanceStatusEnum.ON_TIME, out, shiftEnd))
                    .isEqualTo(AttendanceStatusEnum.EARLY_LEAVE);
        }

        @Test
        @DisplayName("Ra đúng cuối ca → giữ ON_TIME")
        void onTimeKept() {
            assertThat(
                            AttendanceCalc.resolveCheckoutStatus(
                                    AttendanceStatusEnum.ON_TIME, shiftEnd, shiftEnd))
                    .isEqualTo(AttendanceStatusEnum.ON_TIME);
        }

        @Test
        @DisplayName("Vào trễ + ra đúng giờ → giữ LATE")
        void lateKept() {
            assertThat(
                            AttendanceCalc.resolveCheckoutStatus(
                                    AttendanceStatusEnum.LATE, shiftEnd, shiftEnd))
                    .isEqualTo(AttendanceStatusEnum.LATE);
        }

        @Test
        @DisplayName("Ra trong dung sai (5 phút trước cuối ca) → KHÔNG coi là về sớm")
        void withinToleranceNotEarly() {
            LocalDateTime out = shiftEnd.minusMinutes(5);
            assertThat(
                            AttendanceCalc.resolveCheckoutStatus(
                                    AttendanceStatusEnum.ON_TIME, out, shiftEnd))
                    .isEqualTo(AttendanceStatusEnum.ON_TIME);
        }
    }
}
