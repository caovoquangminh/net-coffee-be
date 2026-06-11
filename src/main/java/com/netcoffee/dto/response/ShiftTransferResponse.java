package com.netcoffee.dto.response;

import com.netcoffee.enumtype.ApprovalStatusEnum;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShiftTransferResponse {
    private Long id;
    private Long shiftId;
    private Integer shiftNumber;
    private LocalDateTime shiftDate;
    private Long originalUserId;
    private String originalUserName;
    private Long replacementUserId;
    private String replacementUserName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private ApprovalStatusEnum status;
    private LocalDateTime createdAt;
}
