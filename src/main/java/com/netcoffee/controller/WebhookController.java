package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.request.SePayWebhookRequest;
import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.service.FoodOrderService;
import com.netcoffee.service.QrPaymentService;
import com.netcoffee.utils.ReferenceCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ApiPaths.WEBHOOK)
@RequiredArgsConstructor
public class WebhookController {

    private final QrPaymentService qrPaymentService;
    private final FoodOrderService foodOrderService;

    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<Void>> receivePayment(
            @RequestBody SePayWebhookRequest request) {

        log.info(
                "SePay webhook: type={}, amount={}, content={}",
                request.getTransferType(),
                request.getTransferAmount(),
                request.getContent());

        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }

        String content = request.getContent() != null ? request.getContent() : "";
        String referenceCode = ReferenceCodeUtil.extractFromContent(content);

        if (referenceCode != null) {
            WebhookPaymentRequest webhookRequest = new WebhookPaymentRequest();
            webhookRequest.setTransferContent(content);
            webhookRequest.setTransferAmount(request.getTransferAmount());
            webhookRequest.setBankCode(request.getGateway());
            webhookRequest.setTransactionId(request.getReferenceCode());
            qrPaymentService.processWebhook(webhookRequest);
        } else {
            foodOrderService.autoConfirmPaymentByWebhook(content, request.getTransferAmount());
        }

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
