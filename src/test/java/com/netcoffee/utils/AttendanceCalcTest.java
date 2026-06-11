package com.netcoffee.utils;

import static org.assertj.core.api.Assertions.*;

import com.netcoffee.enumtype.AttendanceStatusEnum;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Logic tính công quán net — bịt kẽ hở phồng giờ/lương. Ca mẫu: 06:00–11:00. */
class AttendanceCalcTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 6, 9, 6, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 6, 9, 11, 0); // ca 5 tiếng

    @Nested
    @DisplayName("resolveCheckIn — dung sai ±15p, trễ quá ghi lateMinutes")
    class CheckIn {
        @Test
        @DisplayName("Vào đúng giờ → ON_TIME, 0")
        void onTime() {
            var r = AttendanceCalc.resolveCheckIn(start, start);
            assertThat(r.status()).isEqualTo(AttendanceStatusEnum.ON_TIME);
            assertThat(r.lateMinutes()).isZero();
        }

        @Test
        @DisplayName("Vào sớm 10p → ON_TIME, 0")
        void early() {
            var r = AttendanceCalc.resolveCheckIn(start.minusMinutes(10), start);
            assertThat(r.status()).isEqualTo(AttendanceStatusEnum.ON_TIME);
            assertThat(r.lateMinutes()).isZero();
        }

        @Test
        @DisplayName("Vào trễ đúng 15p (biên) → vẫn ON_TIME")
        void lateBoundary() {
            var r = AttendanceCalc.resolveCheckIn(start.plusMinutes(15), start);
            assertThat(r.status()).isEqualTo(AttendanceStatusEnum.ON_TIME);
            assertThat(r.lateMinutes()).isZero();
        }

        @Test
        @DisplayName("Vào 06:16 → LATE, lateMinutes = 16")
        void late16() {
            var r = AttendanceCalc.resolveCheckIn(start.plusMinutes(16), start);
            assertThat(r.status()).isEqualTo(AttendanceStatusEnum.LATE);
            assertThat(r.lateMinutes()).isEqualTo(16);
        }

        @Test
        @DisplayName("isTooEarlyToCheckIn: 05:44 quá sớm, 05:45 hợp lệ")
        void tooEarly() {
            assertThat(AttendanceCalc.isTooEarlyToCheckIn(start.minusMinutes(16), start)).isTrue();
            assertThat(AttendanceCalc.isTooEarlyToCheckIn(start.minusMinutes(15), start)).isFalse();
        }
    }

    @Nested
    @DisplayName("check-out — về sớm/trễ cần lý do, early minutes")
    class CheckOut {
        @Test
        @DisplayName("earlyLeaveMinutes: về 10:30 → 30; đúng/quá giờ → 0")
        void earlyLeave() {
            assertThat(AttendanceCalc.earlyLeaveMinutes(end.minusMinutes(30), end)).isEqualTo(30);
            assertThat(AttendanceCalc.earlyLeaveMinutes(end, end)).isZero();
            assertThat(AttendanceCalc.earlyLeaveMinutes(end.plusMinutes(10), end)).isZero();
        }

        @Test
        @DisplayName("checkoutNeedsReason: lệch >15p (sớm hoặc trễ) cần lý do")
        void needsReason() {
            assertThat(AttendanceCalc.checkoutNeedsReason(end, end)).isFalse();
            assertThat(AttendanceCalc.checkoutNeedsReason(end.minusMinutes(15), end)).isFalse();
            assertThat(AttendanceCalc.checkoutNeedsReason(end.minusMinutes(16), end)).isTrue();
            assertThat(AttendanceCalc.checkoutNeedsReason(end.plusMinutes(15), end)).isFalse();
            assertThat(AttendanceCalc.checkoutNeedsReason(end.plusMinutes(16), end)).isTrue();
        }

        @Test
        @DisplayName("resolveCheckoutStatus: về sớm >15p → EARLY_LEAVE; trong dung sai → giữ")
        void status() {
            assertThat(
                            AttendanceCalc.resolveCheckoutStatus(
                                    AttendanceStatusEnum.ON_TIME, end.minusMinutes(30), end))
                    .isEqualTo(AttendanceStatusEnum.EARLY_LEAVE);
            assertThat(AttendanceCalc.resolveCheckoutStatus(AttendanceStatusEnum.LATE, end, end))
                    .isEqualTo(AttendanceStatusEnum.LATE);
            assertThat(
                            AttendanceCalc.resolveCheckoutStatus(
                                    AttendanceStatusEnum.ON_TIME, end.minusMinutes(10), end))
                    .isEqualTo(AttendanceStatusEnum.ON_TIME);
        }
    }

    @Nested
    @DisplayName("workedMinutesInShift — clamp trong ca")
    class Worked {
        @Test
        @DisplayName("Trọn ca 06:00–11:00 → 300 phút")
        void full() {
            assertThat(AttendanceCalc.workedMinutesInShift(start, end, start, end)).isEqualTo(300);
        }

        @Test
        @DisplayName("Vào sớm + ra muộn → vẫn clamp về [06:00,11:00] = 300")
        void clamp() {
            assertThat(
                            AttendanceCalc.workedMinutesInShift(
                                    start.minusMinutes(20), end.plusMinutes(40), start, end))
                    .isEqualTo(300);
        }

        @Test
        @DisplayName("Vào trễ 16p → 284 phút")
        void late() {
            assertThat(AttendanceCalc.workedMinutesInShift(start.plusMinutes(16), end, start, end))
                    .isEqualTo(284);
        }

        @Test
        @DisplayName("null → 0")
        void nulls() {
            assertThat(AttendanceCalc.workedMinutesInShift(null, end, start, end)).isZero();
            assertThat(AttendanceCalc.workedMinutesInShift(start, null, start, end)).isZero();
        }
    }

    @Nested
    @DisplayName("roundShiftHours — 0-14=0, 15-44=0.5, 45-59=1 (theo từng ca)")
    class Round {
        @Test
        @DisplayName("Các mốc spec: 3h12=3.0, 3h20=3.5, 3h46=4.0, 3h55=4.0")
        void specExamples() {
            assertThat(AttendanceCalc.roundShiftHours(192)).isEqualByComparingTo("3.0"); // 3h12
            assertThat(AttendanceCalc.roundShiftHours(200)).isEqualByComparingTo("3.5"); // 3h20
            assertThat(AttendanceCalc.roundShiftHours(226)).isEqualByComparingTo("4.0"); // 3h46
            assertThat(AttendanceCalc.roundShiftHours(235)).isEqualByComparingTo("4.0"); // 3h55
        }

        @Test
        @DisplayName("Biên: 14→0, 15→0.5, 44→0.5, 45→1.0")
        void boundaries() {
            assertThat(AttendanceCalc.roundShiftHours(14)).isEqualByComparingTo("0");
            assertThat(AttendanceCalc.roundShiftHours(15)).isEqualByComparingTo("0.5");
            assertThat(AttendanceCalc.roundShiftHours(44)).isEqualByComparingTo("0.5");
            assertThat(AttendanceCalc.roundShiftHours(45)).isEqualByComparingTo("1.0");
            assertThat(AttendanceCalc.roundShiftHours(60)).isEqualByComparingTo("1.0");
            assertThat(AttendanceCalc.roundShiftHours(300)).isEqualByComparingTo("5.0");
        }

        @Test
        @DisplayName("Phút âm/0 → 0")
        void nonPositive() {
            assertThat(AttendanceCalc.roundShiftHours(0)).isEqualByComparingTo("0");
            assertThat(AttendanceCalc.roundShiftHours(-30)).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("overtimeMinutes — chỉ khi có OT duyệt")
    class Overtime {
        @Test
        void withApprovedOt() {
            assertThat(AttendanceCalc.overtimeMinutes(end.plusMinutes(30), end, true))
                    .isEqualTo(30);
        }

        @Test
        void noOt_zero() {
            assertThat(AttendanceCalc.overtimeMinutes(end.plusMinutes(30), end, false)).isZero();
        }

        @Test
        void notAfterEnd_zero() {
            assertThat(AttendanceCalc.overtimeMinutes(end, end, true)).isZero();
        }
    }
}
