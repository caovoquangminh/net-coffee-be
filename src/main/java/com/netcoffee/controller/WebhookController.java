package com.netcoffee.controller;

import com.netcoffee.dto.request.SePayWebhookRequest;
import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.service.QrPaymentService;
import com.netcoffee.utils.ReferenceCodeUtil;
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
     * SePay gọi endpoint này mỗi khi có giao dịch mới.
     * Chỉ xử lý giao dịch tiền VÀO (transferType = "in").
     */
    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<Void>> receivePayment(
            @RequestBody SePayWebhookRequest request) {

        log.info("SePay webhook: type={}, amount={}, content={}",
                request.getTransferType(),
                request.getTransferAmount(),
                request.getContent());

        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            log.info("Skipping non-incoming transfer");
            return ResponseEntity.ok(ApiResponse.ok(null));
        }

        String referenceCode = ReferenceCodeUtil.extractFromContent(request.getContent());

        if (referenceCode == null) {
            log.info("No referenceCode found in content: {}", request.getContent());
            return ResponseEntity.ok(ApiResponse.ok(null));
        }

        WebhookPaymentRequest webhookRequest = new WebhookPaymentRequest();
        webhookRequest.setTransferContent(request.getContent());
        webhookRequest.setTransferAmount(request.getTransferAmount());
        webhookRequest.setBankCode(request.getGateway());
        webhookRequest.setTransactionId(request.getReferenceCode());

        qrPaymentService.processWebhook(webhookRequest);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}