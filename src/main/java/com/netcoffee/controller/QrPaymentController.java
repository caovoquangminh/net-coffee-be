package com.netcoffee.controller;

import com.netcoffee.dto.request.TopUpRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.QrPaymentResponse;
import com.netcoffee.enumtype.QrPaymentStatusEnum;
import com.netcoffee.service.QrPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/qr-payments") @RequiredArgsConstructor
public class QrPaymentController
{

    private final QrPaymentService qrPaymentService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QrPaymentResponse>> generate(@AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TopUpRequest request)
    {
        Long userId = Long.parseLong(userDetails.getUsername());
        QrPaymentResponse response = qrPaymentService.generateQr(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{referenceCode}/status")
    public ResponseEntity<ApiResponse<QrPaymentStatusEnum>> getStatus(
            @PathVariable String referenceCode
    ) {
        QrPaymentStatusEnum status =
                qrPaymentService.getStatus(referenceCode);

        return ResponseEntity.ok(
                ApiResponse.ok(status)
        );
    }
}
