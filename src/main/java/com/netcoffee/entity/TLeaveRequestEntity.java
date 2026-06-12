package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import com.netcoffee.enumtype.LeaveTypeEnum;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "leave_requests",
        indexes = {
            @Index(name = "idx_leave_user", columnList = "user_id"),
            @Index(name = "idx_leave_date", columnList = "leave_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TLeaveRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Ca xin nghỉ (nếu xin nghỉ theo ca cụ thể); có thể null khi nghỉ cả ngày. */
    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveTypeEnum leaveType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatusEnum status = ApprovalStatusEnum.PENDING;

    @Column(name = "telegram_message_id", length = 100)
    private String telegramMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(AppConstant.VN_ZONE);
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(AppConstant.VN_ZONE);
    }
}
