package com.netcoffee.controller;

import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/users") @RequiredArgsConstructor
public class UserController
{

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserDetails userDetails)
    {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id)
    {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }
}
