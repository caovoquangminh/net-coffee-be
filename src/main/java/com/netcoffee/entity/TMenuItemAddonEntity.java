package com.netcoffee.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(
        name = "menu_item_addons",
        indexes = {@Index(name = "idx_addons_menu_item_id", columnList = "menu_item_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TMenuItemAddonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "extra_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPrice;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    /** true nếu hệ thống tự tắt do hết kho; false nếu admin tắt thủ công. */
    @Column(name = "disabled_by_stock", nullable = false)
    @Builder.Default
    private Boolean disabledByStock = false;

    /** Nguyên liệu kho tương ứng (nullable). Khi kho = 0 thì addon này tự bị tắt. */
    @Column(name = "inventory_item_id")
    private Long inventoryItemId;
}
