package com.netcoffee.service;

import com.netcoffee.dto.request.TopUpRequest;
import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.QrPaymentResponse;
import com.netcoffee.enumtype.QrPaymentStatusEnum;

public interface QrPaymentService {

    QrPaymentResponse generateQr(Long userId, TopUpRequest request);

    /**
     * Tạo QR bằng số điện thoại — không cần đăng nhập. Dùng cho màn hình login khi user muốn nạp
     * tiền trước.
     */
    QrPaymentResponse generateQrByPhone(String phoneNumber, TopUpRequest request);

    void processWebhook(WebhookPaymentRequest request);

    void expireOldQrPayments();

    QrPaymentStatusEnum getStatus(String referenceCode);
}
