package com.netcoffee.controller;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.AdminTopUpRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.CashTopUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints nạp tiền mặt — dùng cho cả ADMIN và STAFF.
 * Mỗi giao dịch ghi lại performedByUserId để tạo audit trail không thể phủ nhận.
 */
@RestController
@RequestMapping("/api/topup")
@RequiredArgsConstructor
public class TopUpController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CashTopUpService cashTopUpService;

    /** Tìm tài khoản khách theo số điện thoại (tối thiểu 3 ký tự). */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchCustomers(
            @RequestParam String phone) {
        if (phone == null || phone.length() < 3) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<TUserEntity> users = userRepository.findByPhoneNumberContainingOrderByCreatedAtDesc(phone);
        return ResponseEntity.ok(ApiResponse.ok(users.stream().map(userMapper::toResponse).toList()));
    }

    /** Nạp tiền mặt. Tự động ghi nhận nhân viên thực hiện từ JWT token. */
    @PostMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<UserResponse>> cashTopUp(
            @PathVariable Long targetUserId,
            @Valid @RequestBody AdminTopUpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long performedById = Long.parseLong(userDetails.getUsername());
        UserResponse updated = cashTopUpService.topUp(
                targetUserId, request.getAmount(), request.getNote(), performedById);

        return ResponseEntity.ok(ApiResponse.ok("Nạp tiền thành công", updated));
    }
}
