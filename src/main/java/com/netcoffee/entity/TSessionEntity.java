package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.SessionStatusEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "sessions",
        indexes = {
            @Index(name = "idx_sessions_user_id", columnList = "user_id"),
            @Index(name = "idx_sessions_machine_id", columnList = "machine_id"),
            @Index(name = "idx_sessions_status", columnList = "status"),
            @Index(name = "idx_sessions_user_status", columnList = "user_id, status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Raw FK thay vì @ManyToOne để tránh N+1 query problem khi chỉ cần ID mà không cần load toàn bộ
     * user object
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "machine_id", nullable = false)
    private Long machineId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SessionStatusEnum status = SessionStatusEnum.ACTIVE;

    @Column(name = "price_plan_id")
    private Long pricePlanId;

    /**
     * Snapshot giá tại thời điểm bắt đầu session. Quan trọng: không tham chiếu trực tiếp price_plan
     * vì giá có thể thay đổi sau.
     */
    @Column(name = "price_per_hour_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerHourSnapshot;

    /**
     * Thời điểm đã bill đến — cập nhật sau mỗi billing tick và sau chargeMinimumFee. Dùng để tính
     * phần lẻ chưa bill khi kết thúc session.
     */
    @Column(name = "last_billed_at")
    private LocalDateTime lastBilledAt;

    /**
     * Thời điểm client gửi heartbeat gần nhất. null = chưa nhận heartbeat nào (phiên cũ trước khi
     * deploy). billingTick dùng field này để phát hiện orphaned sessions.
     */
    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(AppConstant.VN_ZONE);
        if (startedAt == null) {
            startedAt = LocalDateTime.now(AppConstant.VN_ZONE);
        }
    }
}
