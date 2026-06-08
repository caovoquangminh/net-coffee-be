package com.netcoffee.dto.response;

import com.netcoffee.enumtype.PayrollRecordStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollRecordResponse {

    private Long id;
    private Long periodId;
    private Integer year;
    private Integer month;
    private Long userId;
    private String userName;
    private BigDecimal totalHours;
    private BigDecimal hourlyWage;
    private BigDecimal baseSalary;
    private BigDecimal overtimeHours;
    private BigDecimal overtimePay;
    private BigDecimal bonus;
    private BigDecimal penalty;
    private BigDecimal responsibility;
    private BigDecimal advance;
    private BigDecimal totalSalary;
    private PayrollRecordStatusEnum payStatus;
    private String disputeReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
