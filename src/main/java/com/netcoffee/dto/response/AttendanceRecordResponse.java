package com.netcoffee.dto.response;

import com.netcoffee.enumtype.AttendanceStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceRecordResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long shiftId;
    private Integer shiftNumber;
    private LocalDateTime shiftDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private AttendanceStatusEnum attendStatus;
    private BigDecimal hoursWorked;
    private Boolean isOvertime;
    private String note;
    private LocalDateTime createdAt;
}
