package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.PayrollRecordStatusEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "payroll_records",
        indexes = {
            @Index(name = "idx_payroll_rec_period", columnList = "period_id"),
            @Index(name = "idx_payroll_rec_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TPayrollRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_hours", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalHours = BigDecimal.ZERO;

    @Column(name = "hourly_wage", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal hourlyWage = BigDecimal.ZERO;

    @Column(name = "base_salary", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(name = "overtime_hours", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "overtime_pay", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal overtimePay = BigDecimal.ZERO;

    @Column(name = "bonus", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "penalty", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal penalty = BigDecimal.ZERO;

    @Column(name = "responsibility", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal responsibility = BigDecimal.ZERO;

    @Column(name = "advance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal advance = BigDecimal.ZERO;

    @Column(name = "total_salary", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSalary = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_status", nullable = false, length = 20)
    @Builder.Default
    private PayrollRecordStatusEnum payStatus = PayrollRecordStatusEnum.PENDING;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(AppConstant.VN_ZONE);
        updatedAt = LocalDateTime.now(AppConstant.VN_ZONE);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(AppConstant.VN_ZONE);
    }
}
