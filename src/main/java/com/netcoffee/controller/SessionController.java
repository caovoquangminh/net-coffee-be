package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.StartSessionRequest;
import com.netcoffee.dto.response.ActiveSessionWithUserResponse;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.service.SessionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.SESSIONS)
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    private static final String ROLE_ADMIN = "ROLE_" + UserRoleEnum.ADMIN.name();

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
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Session kết thúc", sessionService.endSession(id, userId)));
    }

    @PostMapping("/{id}/force-end")
    public ResponseEntity<ApiResponse<SessionResponse>> forceEndSession(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        boolean isAdmin =
                userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(ROLE_ADMIN));
        if (!isAdmin) {
            SessionResponse existing = sessionService.findById(id);
            if (!existing.getUserId().equals(userId)) {
                throw new AccessDeniedException("Không có quyền kết thúc phiên này");
            }
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Session bị kết thúc", sessionService.forceEndSession(id)));
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
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
    public ResponseEntity<ApiResponse<Page<SessionResponse>>> getMySessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstant.DEFAULT_PAGE_SIZE) int size) {
        Long userId = Long.parseLong(userDetails.getUsername());
        int clampedSize = Math.min(size, AppConstant.MAX_PAGE_SIZE);
        return ResponseEntity.ok(
                ApiResponse.ok(
                        sessionService.findByUserIdPaged(
                                userId, PageRequest.of(page, clampedSize))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.findById(id)));
    }
}
