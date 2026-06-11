package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "shift_swap_requests",
        indexes = {
            @Index(name = "idx_swap_from_user", columnList = "from_user_id"),
            @Index(name = "idx_swap_to_user", columnList = "to_user_id"),
            @Index(name = "idx_swap_shift", columnList = "shift_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TShiftSwapRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người nhường ca. */
    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    /** Người nhận ca. */
    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

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
