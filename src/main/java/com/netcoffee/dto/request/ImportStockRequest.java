package com.netcoffee.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class ImportStockRequest {

    @NotNull(message = "Mã mặt hàng không được trống")
    private Long inventoryItemId;

    @NotNull(message = "Mã nhà cung cấp không được trống")
    private Long supplierId;

    @NotNull
    @DecimalMin(value = "0.001", message = "Số lượng phải lớn hơn 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0", message = "Giá nhập không được âm")
    private BigDecimal purchasePrice;

    private LocalDate expiryDate;

    @Size(max = 500)
    private String notes;
}
