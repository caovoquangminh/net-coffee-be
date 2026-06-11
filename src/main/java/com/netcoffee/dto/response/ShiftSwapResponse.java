package com.netcoffee.dto.response;

import com.netcoffee.enumtype.ApprovalStatusEnum;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShiftSwapResponse {
    private Long id;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private Long shiftId;
    private Integer shiftNumber;
    private LocalDateTime shiftDate;
    private String reason;
    private ApprovalStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
