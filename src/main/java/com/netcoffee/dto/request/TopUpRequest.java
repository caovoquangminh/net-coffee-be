package com.netcoffee.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopUpRequest {

    @NotNull(message = "Số tiền không được trống")
    @DecimalMin(value = "1000", message = "Nạp tối thiểu 1,000 VND")
    private BigDecimal amount;

    private Long machineId;
}
