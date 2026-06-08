package com.netcoffee.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMenuItemRequest {

    private String name;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private String description;
    private Boolean isAvailable;
}
