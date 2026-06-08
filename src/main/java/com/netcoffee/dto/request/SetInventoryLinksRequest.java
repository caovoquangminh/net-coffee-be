package com.netcoffee.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SetInventoryLinksRequest {
    private Long inventoryItemId;
    private BigDecimal quantity;
}
