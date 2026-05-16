package com.netcoffee.controller;

import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.enumtype.QrPaymentStatusEnum;
import com.netcoffee.service.QrPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final QrPaymentService qrPaymentService;

    /**
     * Nhận callback từ SePay / Casso / ngân hàng.
     * Không yêu cầu JWT (permitAll trong SecurityConfig).
     */
    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<Void>> receivePayment(
            @RequestBody WebhookPaymentRequest request) {
        log.info("Webhook received: amount={}, content={}",
                request.getTransferAmount(), request.getTransferContent());
        qrPaymentService.processWebhook(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * FE polling endpoint — check trạng thái QR payment.
     * Không yêu cầu JWT vì nằm dưới /api/webhook/** (permitAll).
     */
    @GetMapping("/payment/status/{referenceCode}")
    public ResponseEntity<ApiResponse<QrPaymentStatusEnum>> getPaymentStatus(
            @PathVariable String referenceCode) {
        QrPaymentStatusEnum status = qrPaymentService.getStatus(referenceCode);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}