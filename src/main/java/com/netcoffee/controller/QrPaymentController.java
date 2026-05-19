package com.netcoffee.controller;

import com.netcoffee.dto.request.TopUpRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.QrPaymentResponse;
import com.netcoffee.enumtype.QrPaymentStatusEnum;
import com.netcoffee.service.QrPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr-payments")
@RequiredArgsConstructor
public class QrPaymentController {

    private final QrPaymentService qrPaymentService;

    /**
     * Tạo QR khi đã đăng nhập
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QrPaymentResponse>> generate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TopUpRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        QrPaymentResponse response = qrPaymentService.generateQr(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * Tạo QR bằng SĐT — không cần đăng nhập.
     * Dùng cho màn hình login khi user muốn nạp tiền trước.
     */
    @PostMapping("/generate-by-phone")
    public ResponseEntity<ApiResponse<QrPaymentResponse>> generateByPhone(
            @RequestParam @NotBlank String phoneNumber,
            @Valid @RequestBody TopUpRequest request) {
        QrPaymentResponse response = qrPaymentService.generateQrByPhone(phoneNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * Check trạng thái QR — FE polling mỗi 3s
     */
    @GetMapping("/{referenceCode}/status")
    public ResponseEntity<ApiResponse<QrPaymentStatusEnum>> getStatus(
            @PathVariable String referenceCode) {
        QrPaymentStatusEnum status = qrPaymentService.getStatus(referenceCode);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}