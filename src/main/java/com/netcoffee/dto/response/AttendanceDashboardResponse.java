package com.netcoffee.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Widget chấm công cho dashboard admin: hôm nay / tuần / tháng. */
@Getter
@Builder
public class AttendanceDashboardResponse {

    // Hôm nay
    private long todayWorking; // đang làm (đã check-in, chưa check-out)
    private long todayLate; // đi trễ
    private long todayOnLeave; // nghỉ phép đã duyệt
    private long todayNotCheckedIn; // đã tới giờ ca nhưng chưa check-in

    // Tuần (Thứ 2 - Chủ nhật hiện tại)
    private BigDecimal weekTotalHours;
    private BigDecimal weekOtHours;
    private int weekFillRatePercent; // tỉ lệ lấp đầy ca (%)

    // Tháng
    private BigDecimal monthSalaryCost;
    private BigDecimal monthTotalHours;
    private List<TopStaff> monthTopStaff;

    @Getter
    @Builder
    public static class TopStaff {
        private Long userId;
        private String userName;
        private BigDecimal hours;
    }
}
