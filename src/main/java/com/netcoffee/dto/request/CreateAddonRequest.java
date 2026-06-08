package com.netcoffee.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAddonRequest {

    private String name;
    private BigDecimal extraPrice;
    private Long inventoryItemId;
    private Boolean isAvailable;
}
