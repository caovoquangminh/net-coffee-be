package com.netcoffee.utils;

import com.netcoffee.enumtype.AttendanceStatusEnum;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Logic tính công thuần (không phụ thuộc Spring/DB) — tách ra để test kỹ và tái dùng cho cả
 * check-out thủ công lẫn job đối soát tự động. Mục tiêu: KHÔNG kẽ hở tính giờ/lương.
 */
public final class AttendanceCalc {

    /** Dung sai để xác định về sớm (phút). Về trước (shiftEnd - dung sai) bị coi là EARLY_LEAVE. */
    public static final long EARLY_LEAVE_TOLERANCE_MINUTES = 10;

    private AttendanceCalc() {}

    /**
     * Thời điểm check-out hiệu lực dùng để tính công. Nếu KHÔNG có OT được duyệt cho ca, giờ làm bị
     * cap tại thời điểm kết thúc ca — nhân viên ở lại quá giờ (hoặc quên check-out) KHÔNG được tính
     * thêm công. Nếu có OT được duyệt, tính tới thời điểm thực tế.
     */
    public static LocalDateTime effectiveCheckOut(
            LocalDateTime actualCheckOut, LocalDateTime shiftEnd, boolean hasApprovedOt) {
        if (hasApprovedOt) {
            return actualCheckOut;
        }
        return actualCheckOut.isAfter(shiftEnd) ? shiftEnd : actualCheckOut;
    }

    /**
     * Số giờ làm việc = (checkOut hiệu lực − checkIn) theo phút, đổi ra giờ, làm tròn 2 chữ số.
     * Không dùng làm tròn 30 phút HALF_UP (gây over-pay khi làm 16 phút thành 0.5h). Trả 0 nếu âm.
     */
    public static BigDecimal workedHours(LocalDateTime checkIn, LocalDateTime effectiveCheckOut) {
        if (checkIn == null || effectiveCheckOut == null) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(checkIn, effectiveCheckOut).toMinutes();
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /**
     * Trạng thái sau check-out. Về sớm hơn (shiftEnd − dung sai) → EARLY_LEAVE; ngược lại giữ trạng
     * thái check-in (ON_TIME/LATE).
     */
    public static AttendanceStatusEnum resolveCheckoutStatus(
            AttendanceStatusEnum checkInStatus,
            LocalDateTime actualCheckOut,
            LocalDateTime shiftEnd) {
        LocalDateTime earlyThreshold = shiftEnd.minusMinutes(EARLY_LEAVE_TOLERANCE_MINUTES);
        if (actualCheckOut.isBefore(earlyThreshold)) {
            return AttendanceStatusEnum.EARLY_LEAVE;
        }
        return checkInStatus;
    }
}
