package com.netcoffee.controller;

import com.netcoffee.dto.request.StartSessionRequest;
import com.netcoffee.dto.response.ActiveSessionWithUserResponse;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
            @Valid @RequestBody StartSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long tokenUserId = Long.parseLong(userDetails.getUsername());
        if (!tokenUserId.equals(request.getUserId())) {
            throw new AccessDeniedException("Không thể mở session cho tài khoản khác");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Session bắt đầu", sessionService.startSession(request)));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<SessionResponse>> endSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Session kết thúc", sessionService.endSession(id, userId)));
    }

    @PostMapping("/{id}/force-end")
    public ResponseEntity<ApiResponse<SessionResponse>> forceEndSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            SessionResponse existing = sessionService.findById(id);
            if (!existing.getUserId().equals(userId)) {
                throw new AccessDeniedException("Không có quyền kết thúc phiên này");
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("Session bị kết thúc", sessionService.forceEndSession(id)));
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        sessionService.heartbeat(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    @GetMapping("/all-active")
    public ResponseEntity<ApiResponse<List<ActiveSessionWithUserResponse>>> getAllActiveSessions() {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.findAllActiveWithUserInfo()));
    }

    // /active and /my must be placed BEFORE /{id} to avoid Spring matching them as IDs
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
