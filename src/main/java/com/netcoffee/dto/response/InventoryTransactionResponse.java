package com.netcoffee.dto.response;

import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InventoryTransactionResponse {
    private Long id;
    private Long inventoryItemId;
    private String inventoryItemName;
    private Long supplierId;
    private String supplierName;
    private InventoryTransactionTypeEnum type;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal purchasePrice;
    private LocalDate expiryDate;
    private String notes;
    private Long performedBy;
    private String performedByName;
    private LocalDateTime createdAt;
}
