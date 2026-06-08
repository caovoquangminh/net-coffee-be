package com.netcoffee.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "menu_item_inventory")
@IdClass(TMenuItemInventoryEntity.PK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TMenuItemInventoryEntity {

    @Id
    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Id
    @Column(name = "inventory_item_id")
    private Long inventoryItemId;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity = BigDecimal.ONE;

    public TMenuItemInventoryEntity(Long menuItemId, Long inventoryItemId) {
        this.menuItemId = menuItemId;
        this.inventoryItemId = inventoryItemId;
        this.quantity = BigDecimal.ONE;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Long menuItemId;
        private Long inventoryItemId;
    }
}
