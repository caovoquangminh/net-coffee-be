package com.netcoffee.service;

import com.netcoffee.dto.request.LoginRequest;
import com.netcoffee.dto.request.RegisterRequest;
import com.netcoffee.dto.response.AuthResponse;
import com.netcoffee.dto.response.UserResponse;

public interface AuthService
{

    AuthResponse login(LoginRequest request);

    UserResponse register(RegisterRequest request);

    void logout(Long userId);
}
