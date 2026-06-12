package com.netcoffee.utils;

import static org.assertj.core.api.Assertions.*;

import com.netcoffee.utils.HandoverRedistribution.AttendanceInput;
import com.netcoffee.utils.HandoverRedistribution.ShiftWindow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Bù handover giữa ca: cộng cho người ở lại, trừ người đến muộn, bảo toàn tổng. */
class HandoverRedistributionTest {

    private final LocalDate d = LocalDate.of(2026, 6, 9);

    private List<ShiftWindow> shifts() {
        return List.of(
                new ShiftWindow(1, 1, d.atTime(6, 0), d.atTime(11, 0)),
                new ShiftWindow(2, 2, d.atTime(11, 0), d.atTime(17, 0)),
                new ShiftWindow(3, 3, d.atTime(17, 0), d.atTime(22, 0)));
    }

    @Test
    @DisplayName("A ở lại 20p, B ca sau đến trễ 20p → A +20, B -20")
    void simpleCover() {
        // A (record 10) ca1, check-out 11:20. B (record 20) ca2, check-in 11:20.
        List<AttendanceInput> recs =
                List.of(
                        new AttendanceInput(10, 1, d.atTime(6, 0), d.atTime(11, 20), false),
                        new AttendanceInput(20, 2, d.atTime(11, 20), d.atTime(17, 0), false));
        Map<Long, Integer> r = HandoverRedistribution.compute(shifts(), recs);
        assertThat(r.get(10L)).isEqualTo(20);
        assertThat(r.get(20L)).isEqualTo(-20);
        assertThat(sum(r)).isZero();
    }

    @Test
    @DisplayName("A ở lại 10p, B trễ 30p → chỉ bù phần trông được = 10")
    void coverCappedByStay() {
        List<AttendanceInput> recs =
                List.of(
                        new AttendanceInput(10, 1, d.atTime(6, 0), d.atTime(11, 10), false),
                        new AttendanceInput(20, 2, d.atTime(11, 30), d.atTime(17, 0), false));
        Map<Long, Integer> r = HandoverRedistribution.compute(shifts(), recs);
        assertThat(r.get(10L)).isEqualTo(10);
        assertThat(r.get(20L)).isEqualTo(-10);
    }

    @Test
    @DisplayName("Người ở lại có OT duyệt → không tính vào bù (tránh trả 2 lần)")
    void approvedOtExcluded() {
        List<AttendanceInput> recs =
                List.of(
                        new AttendanceInput(10, 1, d.atTime(6, 0), d.atTime(11, 20), true),
                        new AttendanceInput(20, 2, d.atTime(11, 20), d.atTime(17, 0), false));
        Map<Long, Integer> r = HandoverRedistribution.compute(shifts(), recs);
        assertThat(r.getOrDefault(10L, 0)).isZero();
        assertThat(r.getOrDefault(20L, 0)).isZero();
    }

    @Test
    @DisplayName("Không ai đến trễ → không bù")
    void noLate() {
        List<AttendanceInput> recs =
                List.of(
                        new AttendanceInput(10, 1, d.atTime(6, 0), d.atTime(11, 20), false),
                        new AttendanceInput(20, 2, d.atTime(11, 0), d.atTime(17, 0), false));
        Map<Long, Integer> r = HandoverRedistribution.compute(shifts(), recs);
        assertThat(r.getOrDefault(10L, 0)).isZero();
    }

    @Test
    @DisplayName(
            "2 người ca sau cùng trễ, 1 người ca trước ở lại → chia trừ theo tỉ lệ, bảo toàn tổng")
    void proportionalDebit() {
        // A ở lại 30p (record 10). B trễ 20p (record 20), C trễ 10p (record 21).
        // covered=min(30,30)=30.
        List<AttendanceInput> recs =
                List.of(
                        new AttendanceInput(10, 1, d.atTime(6, 0), d.atTime(11, 30), false),
                        new AttendanceInput(20, 2, d.atTime(11, 20), d.atTime(17, 0), false),
                        new AttendanceInput(21, 2, d.atTime(11, 10), d.atTime(17, 0), false));
        Map<Long, Integer> r = HandoverRedistribution.compute(shifts(), recs);
        assertThat(r.get(10L)).isEqualTo(30);
        assertThat(r.get(20L) + r.get(21L)).isEqualTo(-30);
        assertThat(sum(r)).isZero();
    }

    @Test
    @DisplayName("Mô phỏng 10 năm điểm danh ngẫu nhiên — bất biến luôn đúng")
    void tenYearSimulation() {
        Random rng = new Random(20260609L);
        long key = 1;
        for (int day = 0; day < 3650; day++) {
            LocalDate date = d.plusDays(day);
            List<ShiftWindow> ws =
                    List.of(
                            new ShiftWindow(1, 1, date.atTime(6, 0), date.atTime(11, 0)),
                            new ShiftWindow(2, 2, date.atTime(11, 0), date.atTime(17, 0)),
                            new ShiftWindow(3, 3, date.atTime(17, 0), date.atTime(22, 0)));
            List<AttendanceInput> recs = new ArrayList<>();
            LocalDateTime[] starts = {date.atTime(6, 0), date.atTime(11, 0), date.atTime(17, 0)};
            LocalDateTime[] ends = {date.atTime(11, 0), date.atTime(17, 0), date.atTime(22, 0)};
            for (int s = 0; s < 3; s++) {
                int people = rng.nextInt(3); // 0..2 người/ca
                for (int p = 0; p < people; p++) {
                    int lateMin = rng.nextInt(40); // 0..39p trễ
                    int stayMin = rng.nextInt(40); // 0..39p ở lại
                    boolean ot = rng.nextInt(10) == 0;
                    LocalDateTime in = starts[s].plusMinutes(lateMin);
                    LocalDateTime out = ends[s].plusMinutes(stayMin);
                    recs.add(new AttendanceInput(key++, s + 1, in, out, ot));
                }
            }
            Map<Long, Integer> r = HandoverRedistribution.compute(ws, recs);

            // Bất biến 1: tổng phút bù = 0 (bảo toàn).
            assertThat(sum(r)).as("conservation day %d", day).isZero();

            // Bất biến 2: giờ công cuối cùng không âm sau khi cộng bù.
            for (AttendanceInput rec : recs) {
                ShiftWindow sw = ws.get((int) rec.shiftId() - 1);
                long inShift =
                        AttendanceCalc.workedMinutesInShift(
                                rec.checkIn(), rec.checkOut(), sw.start(), sw.end());
                long total = inShift + r.getOrDefault(rec.recordKey(), 0);
                assertThat(total).as("non-negative day %d", day).isGreaterThanOrEqualTo(0);
            }
        }
    }

    private static int sum(Map<Long, Integer> m) {
        return m.values().stream().mapToInt(Integer::intValue).sum();
    }
}
