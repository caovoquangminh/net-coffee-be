package com.netcoffee.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class TopUpRequest
{

    @NotNull(message = "Số tiền không được trống") @DecimalMin(value = "1000", message = "Nạp tối thiểu 1,000 VND")
    private BigDecimal amount;

    private Long machineId;
}
