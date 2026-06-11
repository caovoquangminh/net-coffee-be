package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.request.SePayWebhookRequest;
import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.service.FoodOrderService;
import com.netcoffee.service.QrPaymentService;
import com.netcoffee.utils.ReferenceCodeUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ApiPaths.WEBHOOK)
@RequiredArgsConstructor
public class WebhookController {

    private final QrPaymentService qrPaymentService;
    private final FoodOrderService foodOrderService;

    /**
     * Khoá xác thực webhook SePay. SePay gửi kèm header {@code Authorization: Apikey <key>}. Bắt
     * buộc cấu hình ở môi trường thật (nếu để trống thì webhook bị từ chối hoàn toàn để tránh nạp
     * khống).
     */
    @Value("${app.sepay.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<Void>> receivePayment(
            @RequestBody SePayWebhookRequest request, HttpServletRequest httpRequest) {

        if (!isAuthorized(httpRequest)) {
            log.warn(
                    "Rejected unauthorized SePay webhook from {}: content={}, amount={}",
                    httpRequest.getRemoteAddr(),
                    request.getContent(),
                    request.getTransferAmount());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized webhook"));
        }

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

    /**
     * Xác thực webhook bằng header {@code Authorization: Apikey <secret>} của SePay. Fail-closed:
     * nếu chưa cấu hình secret thì từ chối tất cả để tránh nạp tiền khống.
     */
    private boolean isAuthorized(HttpServletRequest httpRequest) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error(
                    "app.sepay.webhook-secret chưa được cấu hình — từ chối webhook để đảm bảo an"
                            + " toàn. Hãy đặt biến môi trường SEPAY_WEBHOOK_SECRET.");
            return false;
        }
        String header = httpRequest.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return false;
        }
        String provided =
                header.startsWith("Apikey ") ? header.substring("Apikey ".length()) : header;
        // So sánh constant-time để tránh timing attack
        return java.security.MessageDigest.isEqual(
                provided.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                webhookSecret.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
