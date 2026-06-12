package com.netcoffee.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bù giờ handover giữa hai ca liền kề (thuần, không phụ thuộc DB — test kỹ).
 *
 * <p>Nghiệp vụ: ranh giới giữa ca (vd 11:00 giữa ca1/ca2). Nếu NV ca SAU check-in trễ (để quán
 * thiếu người) và NV ca TRƯỚC ở lại quá giờ để trông quán hộ, thì phần phút "trông hộ" được:
 *
 * <ul>
 *   <li>CỘNG cho người ở lại (ca trước) — công làm phụ.
 *   <li>TRỪ của người đến muộn (ca sau) — vì đã có người làm thay.
 * </ul>
 *
 * <p>Bảo toàn tổng: tổng phút bù tại mỗi ranh giới = 0. Không tính phần đã được trả bằng OT có đơn
 * duyệt (tránh trả hai lần). Tổng phút trông hộ bị giới hạn bởi tổng phút đến muộn (chỉ bù đúng
 * phần thực sự cần trông).
 */
public final class HandoverRedistribution {

    private HandoverRedistribution() {}

    /** Cửa sổ một ca. */
    public record ShiftWindow(
            long shiftId, int shiftNumber, LocalDateTime start, LocalDateTime end) {}

    /** Dữ liệu chấm công 1 NV cho 1 ca (key = id bản ghi attendance, duy nhất). */
    public record AttendanceInput(
            long recordKey,
            long shiftId,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            boolean hasApprovedOt) {}

    /**
     * Tính phút bù cho từng bản ghi. Trả về map recordKey → phút bù (dương = được cộng, âm = bị
     * trừ). Bản ghi không liên quan sẽ không có trong map (coi như 0).
     */
    public static Map<Long, Integer> compute(
            List<ShiftWindow> shifts, List<AttendanceInput> records) {
        Map<Long, Integer> result = new HashMap<>();
        if (shifts == null || shifts.size() < 2 || records == null || records.isEmpty()) {
            return result;
        }

        List<ShiftWindow> sorted = new ArrayList<>(shifts);
        sorted.sort(Comparator.comparingInt(ShiftWindow::shiftNumber));

        Map<Long, List<AttendanceInput>> byShift = new HashMap<>();
        for (AttendanceInput r : records) {
            byShift.computeIfAbsent(r.shiftId(), k -> new ArrayList<>()).add(r);
        }

        for (int i = 0; i + 1 < sorted.size(); i++) {
            ShiftWindow prev = sorted.get(i);
            ShiftWindow next = sorted.get(i + 1);
            // Chỉ xử lý hai ca liền kề (cuối ca trước = đầu ca sau).
            if (!prev.end().equals(next.start())) {
                continue;
            }
            LocalDateTime boundary = prev.end();

            // Người ca trước ở lại sau ranh giới (không tính nếu đã được trả OT).
            Map<Long, Long> stayMinutes = new HashMap<>();
            for (AttendanceInput r : byShift.getOrDefault(prev.shiftId(), List.of())) {
                if (r.checkOut() == null || r.hasApprovedOt()) {
                    continue;
                }
                long stay = Duration.between(boundary, r.checkOut()).toMinutes();
                if (stay > 0) {
                    stayMinutes.put(r.recordKey(), stay);
                }
            }

            // Người ca sau đến muộn sau ranh giới.
            Map<Long, Long> lateMinutes = new HashMap<>();
            for (AttendanceInput r : byShift.getOrDefault(next.shiftId(), List.of())) {
                if (r.checkIn() == null) {
                    continue;
                }
                long late = Duration.between(boundary, r.checkIn()).toMinutes();
                if (late > 0) {
                    lateMinutes.put(r.recordKey(), late);
                }
            }

            long totalStay = stayMinutes.values().stream().mapToLong(Long::longValue).sum();
            long totalLate = lateMinutes.values().stream().mapToLong(Long::longValue).sum();
            long covered = Math.min(totalStay, totalLate);
            if (covered <= 0) {
                continue;
            }

            // Cộng cho người ở lại (chia theo tỉ lệ phút ở lại).
            for (Map.Entry<Long, Integer> e : splitProportional(stayMinutes, covered).entrySet()) {
                result.merge(e.getKey(), e.getValue(), Integer::sum);
            }
            // Trừ của người đến muộn (chia theo tỉ lệ phút đến muộn).
            for (Map.Entry<Long, Integer> e : splitProportional(lateMinutes, covered).entrySet()) {
                result.merge(e.getKey(), -e.getValue(), Integer::sum);
            }
        }
        return result;
    }

    /**
     * Chia số nguyên {@code total} cho các key theo trọng số, dùng phương pháp dư lớn nhất (largest
     * remainder) để tổng phần chia đúng bằng {@code total}.
     */
    static Map<Long, Integer> splitProportional(Map<Long, Long> weights, long total) {
        Map<Long, Integer> out = new HashMap<>();
        long sumW = weights.values().stream().mapToLong(Long::longValue).sum();
        if (sumW <= 0 || total <= 0) {
            return out;
        }
        long assigned = 0;
        List<Map.Entry<Long, Double>> remainders = new ArrayList<>();
        for (Map.Entry<Long, Long> e : weights.entrySet()) {
            double exact = (double) total * e.getValue() / sumW;
            int floor = (int) Math.floor(exact);
            out.put(e.getKey(), floor);
            assigned += floor;
            remainders.add(Map.entry(e.getKey(), exact - floor));
        }
        long leftover = total - assigned;
        remainders.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < leftover && i < remainders.size(); i++) {
            Long key = remainders.get(i).getKey();
            out.merge(key, 1, Integer::sum);
        }
        return out;
    }
}
