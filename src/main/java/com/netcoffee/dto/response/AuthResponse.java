package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class AuthResponse
{

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserResponse user;
}
