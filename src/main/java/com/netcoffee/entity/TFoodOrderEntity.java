package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.enumtype.FoodOrderPaymentEnum;
import com.netcoffee.enumtype.OrderStatusEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "orders",
        indexes = {
            @Index(name = "idx_orders_session_id", columnList = "session_id"),
            @Index(name = "idx_orders_user_id", columnList = "user_id"),
            @Index(name = "idx_orders_machine_id", columnList = "machine_id"),
            @Index(name = "idx_orders_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TFoodOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "machine_id", nullable = false)
    private Long machineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatusEnum status = OrderStatusEnum.PENDING;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "note", length = 500)
    private String note;

    /** ID nhân viên/admin xác nhận đơn */
    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private FoodOrderPaymentEnum paymentMethod = FoodOrderPaymentEnum.CASH;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(AppConstant.VN_ZONE);
    }
}
