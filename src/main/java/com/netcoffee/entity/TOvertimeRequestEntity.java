package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.OvertimeStatusEnum;
import com.netcoffee.enumtype.OvertimeTypeEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "overtime_requests",
        indexes = {@Index(name = "idx_ot_req_requester", columnList = "requester_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TOvertimeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "ot_type", nullable = false, length = 20)
    private OvertimeTypeEnum otType;

    @Column(name = "covering_user_id")
    private Long coveringUserId;

    /** Người được nhờ làm thay (nếu OT là làm thay cho ai đó). */
    @Column(name = "replacement_user_id")
    private Long replacementUserId;

    /** Giờ OT cụ thể (bắt buộc với spec mới: ngày + giờ bắt đầu/kết thúc). */
    @Column(name = "ot_start_time")
    private LocalDateTime otStartTime;

    @Column(name = "ot_end_time")
    private LocalDateTime otEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OvertimeStatusEnum status = OvertimeStatusEnum.PENDING;

    @Column(name = "telegram_message_id", length = 100)
    private String telegramMessageId;

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
