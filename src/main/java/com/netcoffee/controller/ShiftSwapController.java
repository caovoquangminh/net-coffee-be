package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.ShiftSwapResponse;
import com.netcoffee.service.ShiftSwapService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.SHIFT_SWAP)
@RequiredArgsConstructor
public class ShiftSwapController {

    private final ShiftSwapService shiftSwapService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ShiftSwapResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = hasRole(userDetails, "ROLE_ADMIN");
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(shiftSwapService.list(userId, isAdmin)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long fromUserId = Long.parseLong(userDetails.getUsername());
        Long toUserId = Long.valueOf(body.get("toUserId").toString());
        Long shiftId = Long.valueOf(body.get("shiftId").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Đã gửi yêu cầu đổi ca",
                        shiftSwapService.create(fromUserId, toUserId, shiftId, reason)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã duyệt đổi ca", shiftSwapService.approve(id)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối đổi ca", shiftSwapService.reject(id)));
    }

    private boolean hasRole(UserDetails u, String role) {
        return u.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
