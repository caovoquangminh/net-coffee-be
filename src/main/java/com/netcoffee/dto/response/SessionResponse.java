package com.netcoffee.dto.response;

import com.netcoffee.enumtype.SessionStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class SessionResponse
{

    private Long id;
    private Long userId;
    private Long machineId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationSeconds;
    private BigDecimal totalCost;
    private SessionStatusEnum status;
    private BigDecimal pricePerHourSnapshot;
}
