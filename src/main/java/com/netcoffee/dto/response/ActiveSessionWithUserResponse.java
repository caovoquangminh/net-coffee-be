package com.netcoffee.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

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
