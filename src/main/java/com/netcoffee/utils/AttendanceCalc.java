package com.netcoffee.utils;

import com.netcoffee.enumtype.AttendanceStatusEnum;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Logic tính công thuần (không phụ thuộc Spring/DB) — tách ra để test kỹ và tái dùng cho cả
 * check-in/out thủ công lẫn job đối soát tự động. Mục tiêu: KHÔNG kẽ hở tính giờ/lương.
 *
 * <p>Nghiệp vụ quán net:
 *
 * <ul>
 *   <li>Check-in cho phép sớm/trễ trong {@value #CHECK_IN_TOLERANCE_MIN} phút; trễ quá vẫn cho vào
 *       nhưng ghi {@code lateMinutes} và đánh dấu LATE.
 *   <li>Check-out sớm/trễ quá {@value #CHECK_OUT_TOLERANCE_MIN} phút phải kèm lý do.
 *   <li>Giờ công chỉ tính TRONG phạm vi ca (clamp). Phần ngoài ca → OT (nếu có đơn duyệt) hoặc bù
 *       handover giữa ca.
 *   <li>Làm tròn theo từng ca: 0–14p = 0; 15–44p = 0.5h; 45–59p = 1h. Tổng = Σ giờ đã tròn.
 * </ul>
 */
public final class AttendanceCalc {

    /** Dung sai check-in (phút): trong [start-15, start+15] coi là đúng giờ. */
    public static final long CHECK_IN_TOLERANCE_MIN = 15;

    /** Dung sai check-out (phút): sớm/trễ quá mức này cần lý do. */
    public static final long CHECK_OUT_TOLERANCE_MIN = 15;

    private AttendanceCalc() {}

    // ------------------------------------------------------------------ check-in

    /** Kết quả phân giải check-in. */
    public record CheckInResult(AttendanceStatusEnum status, int lateMinutes) {}

    /**
     * Phân giải trạng thái + số phút trễ khi check-in. Trễ quá dung sai → LATE và {@code
     * lateMinutes} = số phút trễ thực tế so với giờ bắt đầu ca (vd vào 06:16, ca 06:00 → 16). Trong
     * dung sai (kể cả vào sớm) → ON_TIME, lateMinutes = 0.
     */
    public static CheckInResult resolveCheckIn(LocalDateTime checkIn, LocalDateTime shiftStart) {
        long diff = Duration.between(shiftStart, checkIn).toMinutes(); // >0 nếu vào sau giờ ca
        if (diff > CHECK_IN_TOLERANCE_MIN) {
            return new CheckInResult(AttendanceStatusEnum.LATE, (int) diff);
        }
        return new CheckInResult(AttendanceStatusEnum.ON_TIME, 0);
    }

    /** Quá sớm so với cho phép (trước start - dung sai) → chưa được check-in. */
    public static boolean isTooEarlyToCheckIn(LocalDateTime now, LocalDateTime shiftStart) {
        return now.isBefore(shiftStart.minusMinutes(CHECK_IN_TOLERANCE_MIN));
    }

    // ----------------------------------------------------------------- check-out

    /** Số phút về sớm so với cuối ca (0 nếu đủ/quá giờ). */
    public static int earlyLeaveMinutes(LocalDateTime actualCheckOut, LocalDateTime shiftEnd) {
        long diff =
                Duration.between(actualCheckOut, shiftEnd).toMinutes(); // >0 nếu về trước cuối ca
        return diff > 0 ? (int) diff : 0;
    }

    /** Check-out sớm/trễ quá dung sai → cần lý do bắt buộc. */
    public static boolean checkoutNeedsReason(
            LocalDateTime actualCheckOut, LocalDateTime shiftEnd) {
        long diff = Duration.between(shiftEnd, actualCheckOut).toMinutes(); // <0 sớm, >0 trễ
        return Math.abs(diff) > CHECK_OUT_TOLERANCE_MIN;
    }

    /**
     * Trạng thái sau check-out: về sớm hơn (cuối ca − dung sai) → EARLY_LEAVE; ngược lại giữ trạng
     * thái check-in (ON_TIME/LATE).
     */
    public static AttendanceStatusEnum resolveCheckoutStatus(
            AttendanceStatusEnum checkInStatus,
            LocalDateTime actualCheckOut,
            LocalDateTime shiftEnd) {
        if (actualCheckOut.isBefore(shiftEnd.minusMinutes(CHECK_OUT_TOLERANCE_MIN))) {
            return AttendanceStatusEnum.EARLY_LEAVE;
        }
        return checkInStatus;
    }

    // -------------------------------------------------------------- tính giờ công

    /**
     * Số phút làm việc TRONG ca = giao của [checkIn, checkOut] với [shiftStart, shiftEnd]. Vào sớm
     * không được tính trước giờ ca; ở lại sau giờ ca không tính vào giờ thường (phần đó là OT/bù).
     */
    public static long workedMinutesInShift(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            LocalDateTime shiftStart,
            LocalDateTime shiftEnd) {
        if (checkIn == null || checkOut == null) {
            return 0;
        }
        LocalDateTime start = checkIn.isAfter(shiftStart) ? checkIn : shiftStart;
        LocalDateTime end = checkOut.isBefore(shiftEnd) ? checkOut : shiftEnd;
        long minutes = Duration.between(start, end).toMinutes();
        return Math.max(0, minutes);
    }

    /**
     * Số phút OT = phần làm SAU cuối ca, chỉ khi có OT được duyệt. Nếu không có OT duyệt, phần dư
     * này = 0 ở đây (có thể được xử lý bằng bù handover ở {@link HandoverRedistribution}).
     */
    public static long overtimeMinutes(
            LocalDateTime checkOut, LocalDateTime shiftEnd, boolean hasApprovedOt) {
        if (!hasApprovedOt || checkOut == null || !checkOut.isAfter(shiftEnd)) {
            return 0;
        }
        return Duration.between(shiftEnd, checkOut).toMinutes();
    }

    /**
     * Làm tròn giờ của MỘT ca theo phút: 0–14 → 0; 15–44 → 0.5h; 45–59 → +1h. Áp dụng cho tổng phút
     * công của ca (đã gồm bù handover). Phút âm → 0.
     */
    public static BigDecimal roundShiftHours(long minutes) {
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        long whole = minutes / 60;
        long rem = minutes % 60;
        double frac;
        if (rem <= 14) {
            frac = 0.0;
        } else if (rem <= 44) {
            frac = 0.5;
        } else {
            frac = 1.0;
        }
        return BigDecimal.valueOf(whole + frac);
    }
}
