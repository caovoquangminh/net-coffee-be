package com.netcoffee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity @Table(name = "order_items", indexes = { @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_item_id", columnList = "item_id") }) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TFoodOrderItemEntity
{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Snapshot giá tại thời điểm đặt món. Tương tự price_per_hour_snapshot
     * trong session — không đổi dù menu thay đổi sau.
     */
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;
}
