package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private UserResponse user;

    /**
     * Session được tạo tự động khi login.
     * Null nếu máy bận hoặc user không đủ tiền.
     */
    private SessionResponse session;
}