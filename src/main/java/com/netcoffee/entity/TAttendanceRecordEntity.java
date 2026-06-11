package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.AttendanceStatusEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "attendance_records",
        indexes = {
            @Index(name = "idx_attendance_user_id", columnList = "user_id"),
            @Index(name = "idx_attendance_shift_id", columnList = "shift_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TAttendanceRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "attend_status", length = 20)
    private AttendanceStatusEnum attendStatus;

    @Column(name = "hours_worked", precision = 5, scale = 2)
    private BigDecimal hoursWorked;

    /** Số phút đi trễ so với giờ bắt đầu ca (0 nếu đúng giờ/sớm). */
    @Column(name = "late_minutes", nullable = false)
    @Builder.Default
    private Integer lateMinutes = 0;

    /** Số phút về sớm so với giờ kết thúc ca (0 nếu đủ/quá giờ). */
    @Column(name = "early_leave_minutes", nullable = false)
    @Builder.Default
    private Integer earlyLeaveMinutes = 0;

    /** Phút được bù/bị trừ do handover giữa ca (dương=được cộng, âm=bị trừ). */
    @Column(name = "redistributed_minutes", nullable = false)
    @Builder.Default
    private Integer redistributedMinutes = 0;

    /** Lý do bắt buộc khi check-out sớm/trễ quá dung sai. */
    @Column(name = "checkout_reason", length = 500)
    private String checkoutReason;

    @Column(name = "is_overtime", nullable = false)
    @Builder.Default
    private Boolean isOvertime = false;

    @Column(name = "overtime_request_id")
    private Long overtimeRequestId;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(AppConstant.VN_ZONE);
    }
}
