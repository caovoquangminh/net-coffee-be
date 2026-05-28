package com.netcoffee.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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
