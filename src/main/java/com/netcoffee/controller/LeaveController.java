package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.LeaveRequestResponse;
import com.netcoffee.enumtype.LeaveTypeEnum;
import com.netcoffee.service.LeaveService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.LEAVE)
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = hasRole(userDetails, "ROLE_ADMIN");
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(leaveService.list(userId, isAdmin)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long shiftId =
                body.get("shiftId") != null ? Long.valueOf(body.get("shiftId").toString()) : null;
        LocalDate date = LocalDate.parse(body.get("leaveDate").toString());
        LeaveTypeEnum type = LeaveTypeEnum.valueOf(body.get("leaveType").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Đã gửi đơn nghỉ",
                        leaveService.create(userId, shiftId, date, type, reason)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã duyệt đơn nghỉ", leaveService.approve(id)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối đơn nghỉ", leaveService.reject(id)));
    }

    private boolean hasRole(UserDetails u, String role) {
        return u.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
