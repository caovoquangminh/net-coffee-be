package com.netcoffee.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStaffProfileRequest {

    private String fullName;
    private String avatarUrl;
    private String staffAddress;
    private String idCard;
    private String staffEmail;
    private LocalDate birthDate;
    private LocalDate startDate;
    private BigDecimal hourlyWage;
}
