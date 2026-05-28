package com.netcoffee.dto.response;

import com.netcoffee.enumtype.SessionStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionResponse {

    private Long id;
    private Long userId;
    private Long machineId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationSeconds;
    private BigDecimal totalCost;
    private SessionStatusEnum status;
    private BigDecimal pricePerHourSnapshot;
    private Boolean isFree;
    private LocalDateTime lastBilledAt;
}
