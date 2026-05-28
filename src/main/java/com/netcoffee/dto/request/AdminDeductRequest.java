package com.netcoffee.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDeductRequest {

    @NotNull(message = "Số tiền không được trống")
    @DecimalMin(value = "1000", message = "Trừ tối thiểu 1,000 VND")
    private BigDecimal amount;

    @NotBlank(message = "Lý do trừ tiền không được để trống")
    private String reason;
}
