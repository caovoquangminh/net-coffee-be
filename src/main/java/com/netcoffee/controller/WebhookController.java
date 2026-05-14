package com.netcoffee.controller;

import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.service.QrPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j @RestController @RequestMapping("/api/webhook") @RequiredArgsConstructor
public class WebhookController
{

    private final QrPaymentService qrPaymentService;

    /**
     * Endpoint nhận callback từ SePay / Casso / ngân hàng Không yêu cầu JWT (đã
     * cấu hình permitAll trong SecurityConfig) Nên thêm secret header để verify
     * nguồn gửi ở production
     */
    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<Void>> receivePayment(@RequestBody WebhookPaymentRequest request)
    {
        log.info("Webhook received: amount={}, content={}", request.getTransferAmount(), request.getTransferContent());
        qrPaymentService.processWebhook(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
