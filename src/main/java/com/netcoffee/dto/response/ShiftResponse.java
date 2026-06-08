package com.netcoffee.dto.response;

import com.netcoffee.enumtype.ShiftRegistrationStatusEnum;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShiftResponse {

    private Long id;
    private Integer shiftNumber;
    private LocalDate shiftDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<RegistrationInfo> registrations;

    @Getter
    @Builder
    public static class RegistrationInfo {
        private Long registrationId;
        private Long userId;
        private String userName;
        private ShiftRegistrationStatusEnum status;
        private LocalDateTime createdAt;
    }
}
