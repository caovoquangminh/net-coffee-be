package com.netcoffee.dto.response;

import com.netcoffee.enumtype.SessionStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionHistoryResponse {

    private Long id;
    private Long userId;
    private String phoneNumber;
    private String fullName;
    private Long machineId;
    private String machineCode;
    private String machineName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationSeconds;
    private BigDecimal totalCost;
    private SessionStatusEnum status;
    private Boolean isFree;
}
