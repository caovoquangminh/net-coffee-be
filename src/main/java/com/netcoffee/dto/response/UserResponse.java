package com.netcoffee.dto.response;

import com.netcoffee.enumtype.UserRoleEnum;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class UserResponse
{

    private Long id;
    private String phoneNumber;
    private String fullName;
    private String avatarUrl;
    private BigDecimal balance;
    private BigDecimal totalSpent;
    private Boolean isActive;
    private UserRoleEnum role;
    private LocalDateTime createdAt;
}
