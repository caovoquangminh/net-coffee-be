package com.netcoffee.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CreateInventoryItemRequest {

    @NotBlank(message = "Tên mặt hàng không được trống")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Đơn vị tính không được trống")
    @Size(max = 30)
    private String unit;

    @DecimalMin(value = "0", message = "Tồn kho tối thiểu không được âm")
    private BigDecimal minStock = BigDecimal.ZERO;

    private Long menuItemId;

    @Size(max = 500)
    private String description;
}
