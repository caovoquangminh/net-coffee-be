package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.AssignmentSourceEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Đoạn ca thực tế của một nhân viên (sau khi chia ca do làm thay). Check-in/out & tính công dựa vào
 * đây.
 */
@Entity
@Table(
        name = "shift_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"shift_id", "user_id"}),
        indexes = {
            @Index(name = "idx_assignment_shift", columnList = "shift_id"),
            @Index(name = "idx_assignment_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TShiftAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private AssignmentSourceEnum source = AssignmentSourceEnum.REGISTRATION;

    @Column(name = "transfer_request_id")
    private Long transferRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(AppConstant.VN_ZONE);
    }
}
