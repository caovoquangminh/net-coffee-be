package com.netcoffee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_addons", indexes = {
        @Index(name = "idx_order_item_addons_order_item_id", columnList = "order_item_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TFoodOrderItemAddonEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "addon_id", nullable = false)
    private Long addonId;

    /** Snapshot tên addon tại thời điểm đặt */
    @Column(name = "addon_name", nullable = false, length = 100)
    private String addonName;

    /** Snapshot giá addon tại thời điểm đặt */
    @Column(name = "addon_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal addonPrice;
}
