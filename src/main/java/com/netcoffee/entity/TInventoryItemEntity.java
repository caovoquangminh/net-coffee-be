package com.netcoffee.entity;

import com.netcoffee.constant.AppConstant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TInventoryItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Đơn vị: gói, lon, cái, kg, lít, ... */
    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "current_stock", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal currentStock = BigDecimal.ZERO;

    /** Ngưỡng cảnh báo sắp hết hàng */
    @Column(name = "min_stock", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal minStock = BigDecimal.ZERO;

    /** Liên kết với menu item — nullable (có thể là nguyên liệu thô không có trong menu) */
    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(AppConstant.VN_ZONE);
    }
}
