package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.AdminDeductRequest;
import com.netcoffee.dto.request.CreateCustomerRequest;
import com.netcoffee.dto.request.ResetPasswordRequest;
import com.netcoffee.dto.request.UpdateUserRequest;
import com.netcoffee.dto.response.*;
import com.netcoffee.service.AdminService;
import com.netcoffee.service.UserManagementService;
import com.netcoffee.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.ADMIN)
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final UserManagementService userManagementService;

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam(defaultValue = "") String phone,
            @AuthenticationPrincipal UserDetails principal) {

        boolean isAdmin =
                principal.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(ApiResponse.ok(adminService.searchUsers(phone, isAdmin)));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                "Tạo tài khoản hội viên thành công",
                                userManagementService.createCustomer(request)));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật thành công", userManagementService.adminUpdateUser(id, request)));
    }

    @PostMapping("/users/{id}/deduct")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> deductBalance(
            @PathVariable Long id,
            @Valid @RequestBody AdminDeductRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Trừ tiền thành công", adminService.deductBalance(id, request, adminId)));
    }

    @PutMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userManagementService.adminResetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công", null));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AdminTransactionResponse>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstant.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTransactions(page, size)));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<SessionHistoryResponse>>> getSessionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstant.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getSessionHistory(page, size)));
    }
}
