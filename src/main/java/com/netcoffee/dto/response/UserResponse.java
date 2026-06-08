package com.netcoffee.dto.response;

import com.netcoffee.enumtype.UserRoleEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String phoneNumber;
    private String fullName;
    private String avatarUrl;
    private BigDecimal balance;
    private BigDecimal totalSpent;
    private Boolean isActive;
    private UserRoleEnum role;
    private LocalDateTime createdAt;

    // Staff profile fields
    private String staffAddress;
    private String idCard;
    private String staffEmail;
    private LocalDate birthDate;
    private LocalDate startDate;
    private BigDecimal hourlyWage;
    private LocalDateTime deletedAt;
}
