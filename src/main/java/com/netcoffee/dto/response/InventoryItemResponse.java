package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class InventoryItemResponse {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal minStock;
    private boolean outOfStock;
    private boolean lowStock;
    private Long menuItemId;
    private String menuItemName;
    private String description;
    private LocalDateTime createdAt;
}
