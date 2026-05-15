package com.netcoffee.service;

import com.netcoffee.dto.request.TopUpRequest;
import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.QrPaymentResponse;
import com.netcoffee.enumtype.QrPaymentStatusEnum;

public interface QrPaymentService
{

    QrPaymentResponse generateQr(Long userId, TopUpRequest request);

    /**
     * Được gọi khi ngân hàng bắn webhook về Parse nội dung CK để match
     * referenceCode và cộng tiền
     */
    void processWebhook(WebhookPaymentRequest request);

    void expireOldQrPayments();
    QrPaymentStatusEnum getStatus(String referenceCode);
}
