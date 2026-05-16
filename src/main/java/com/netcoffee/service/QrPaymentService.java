package com.netcoffee.service;

import com.netcoffee.dto.request.TopUpRequest;
import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.QrPaymentResponse;
import com.netcoffee.enumtype.QrPaymentStatusEnum;

public interface QrPaymentService {

    QrPaymentResponse generateQr(Long userId, TopUpRequest request);

    void processWebhook(WebhookPaymentRequest request);

    void expireOldQrPayments();

    /**
     * FE gọi để polling trạng thái QR payment.
     * Trả về EXPIRED nếu không tìm thấy referenceCode.
     */
    QrPaymentStatusEnum getStatus(String referenceCode);
}