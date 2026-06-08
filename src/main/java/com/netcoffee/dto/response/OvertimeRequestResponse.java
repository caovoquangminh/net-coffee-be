package com.netcoffee.dto.response;

import com.netcoffee.enumtype.OvertimeStatusEnum;
import com.netcoffee.enumtype.OvertimeTypeEnum;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OvertimeRequestResponse {

    private Long id;
    private Long requesterId;
    private String requesterName;
    private Long shiftId;
    private Integer shiftNumber;
    private LocalDateTime shiftDate;
    private String reason;
    private OvertimeTypeEnum otType;
    private Long coveringUserId;
    private String coveringUserName;
    private OvertimeStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
