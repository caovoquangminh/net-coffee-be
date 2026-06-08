package com.netcoffee.dto.response;

import com.netcoffee.enumtype.PayrollPeriodStatusEnum;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollPeriodResponse {

    private Long id;
    private Integer year;
    private Integer month;
    private PayrollPeriodStatusEnum status;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
