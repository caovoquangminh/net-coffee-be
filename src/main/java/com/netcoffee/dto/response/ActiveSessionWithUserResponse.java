package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ActiveSessionWithUserResponse {
    private Long sessionId;
    private Long machineId;
    private Long userId;
    private String phoneNumber;
    private String fullName;
    private LocalDateTime startedAt;
    private BigDecimal pricePerHourSnapshot;
    private BigDecimal balance;
    private Boolean isFree;
}
