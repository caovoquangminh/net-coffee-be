package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.ShiftTransferResponse;
import com.netcoffee.service.ShiftTransferService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.SHIFT_TRANSFER)
@RequiredArgsConstructor
public class ShiftTransferController {

    private final ShiftTransferService shiftTransferService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ShiftTransferResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = hasRole(userDetails, "ROLE_ADMIN");
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(shiftTransferService.list(userId, isAdmin)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ShiftTransferResponse>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUser = Long.parseLong(userDetails.getUsername());
        // 2 dạng:
        //  - "Nhờ người làm thay": originalUserId = mình (mặc định), replacementUserId = đồng
        // nghiệp.
        //  - "Làm thay ai đó": originalUserId = chủ ca, replacementUserId = mình.
        Long originalUserId =
                body.get("originalUserId") != null
                        ? Long.valueOf(body.get("originalUserId").toString())
                        : currentUser;
        Long shiftId = Long.valueOf(body.get("shiftId").toString());
        Long replacementUserId = Long.valueOf(body.get("replacementUserId").toString());
        if (!currentUser.equals(originalUserId) && !currentUser.equals(replacementUserId)) {
            throw new IllegalArgumentException("Bạn phải là một bên trong yêu cầu làm thay.");
        }
        LocalDateTime start = LocalDateTime.parse(body.get("startTime").toString());
        LocalDateTime end = LocalDateTime.parse(body.get("endTime").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Đã gửi yêu cầu làm thay",
                        shiftTransferService.create(
                                originalUserId, shiftId, replacementUserId, start, end, reason)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftTransferResponse>> approve(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Đã duyệt làm thay", shiftTransferService.approve(id, adminId)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftTransferResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Đã từ chối làm thay", shiftTransferService.reject(id)));
    }

    private boolean hasRole(UserDetails u, String role) {
        return u.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
