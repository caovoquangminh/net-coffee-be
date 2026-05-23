package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

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
}
