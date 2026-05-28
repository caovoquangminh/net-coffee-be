package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.request.AdminTopUpRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.service.CashTopUpService;
import com.netcoffee.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.TOPUP)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class TopUpController {

    private final UserManagementService userManagementService;
    private final CashTopUpService cashTopUpService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchCustomers(
            @RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.searchCustomers(phone)));
    }

    @PostMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<UserResponse>> cashTopUp(
            @PathVariable Long targetUserId,
            @Valid @RequestBody AdminTopUpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long performedById = Long.parseLong(userDetails.getUsername());
        UserResponse updated =
                cashTopUpService.topUp(
                        targetUserId, request.getAmount(), request.getNote(), performedById);

        return ResponseEntity.ok(ApiResponse.ok("Nạp tiền thành công", updated));
    }
}
