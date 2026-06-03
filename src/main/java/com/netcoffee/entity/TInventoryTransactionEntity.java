package com.netcoffee.entity;

import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.*;

@Entity
@Table(
        name = "inventory_transactions",
        indexes = {
            @Index(name = "idx_inv_tx_item_id", columnList = "inventory_item_id"),
            @Index(name = "idx_inv_tx_type", columnList = "type"),
            @Index(name = "idx_inv_tx_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TInventoryTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    /** Null khi xuất hàng / điều chỉnh */
    @Column(name = "supplier_id")
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InventoryTransactionTypeEnum type;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    /** Giá nhập (null khi xuất) */
    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    /** Hạn sử dụng (null nếu không có) */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
