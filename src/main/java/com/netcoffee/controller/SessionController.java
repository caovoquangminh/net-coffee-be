package com.netcoffee.controller;

import com.netcoffee.dto.request.StartSessionRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SessionResponse>> startSession(
            @Valid @RequestBody StartSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Session bắt đầu", sessionService.startSession(request)));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<SessionResponse>> endSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Session kết thúc", sessionService.endSession(id)));
    }

    @PostMapping("/{id}/force-end")
    public ResponseEntity<ApiResponse<SessionResponse>> forceEndSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Session bị kết thúc", sessionService.forceEndSession(id)));
    }

    // ← /active và /my phải đặt TRƯỚC /{id} để tránh Spring match nhầm
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SessionResponse>> getActiveSession(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(sessionService.findActiveByUserId(userId)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getMySessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(sessionService.findByUserId(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.findById(id)));
    }
}