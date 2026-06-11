package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "shift_transfer_requests",
        indexes = {
            @Index(name = "idx_transfer_shift", columnList = "shift_id"),
            @Index(name = "idx_transfer_original", columnList = "original_user_id"),
            @Index(name = "idx_transfer_replace", columnList = "replacement_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TShiftTransferRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    /** A — người nhờ làm thay. */
    @Column(name = "original_user_id", nullable = false)
    private Long originalUserId;

    /** B — người làm thay. */
    @Column(name = "replacement_user_id", nullable = false)
    private Long replacementUserId;

    /** Đoạn B làm thay. */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatusEnum status = ApprovalStatusEnum.PENDING;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

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
