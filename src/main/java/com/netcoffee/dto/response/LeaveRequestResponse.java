package com.netcoffee.dto.response;

import com.netcoffee.enumtype.ApprovalStatusEnum;
import com.netcoffee.enumtype.LeaveTypeEnum;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveRequestResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long shiftId;
    private Integer shiftNumber;
    private LocalDate leaveDate;
    private LeaveTypeEnum leaveType;
    private String reason;
    private ApprovalStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
