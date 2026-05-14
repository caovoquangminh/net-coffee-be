package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.TopUpRequest;
import com.netcoffee.dto.request.WebhookPaymentRequest;
import com.netcoffee.dto.response.QrPaymentResponse;
import com.netcoffee.entity.TQrPaymentEntity;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.QrPaymentStatusEnum;
import com.netcoffee.repository.QrPaymentRepository;
import com.netcoffee.service.QrPaymentService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import com.netcoffee.utils.ReferenceCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrPaymentServiceImpl implements QrPaymentService {

    private final QrPaymentRepository qrPaymentRepository;
    private final UserService userService;
    private final TransactionService transactionService;

    @Override
    @Transactional
    public QrPaymentResponse generateQr(Long userId, TopUpRequest request) {
        // Đảm bảo user tồn tại
        userService.getEntityById(userId);

        String referenceCode = ReferenceCodeUtil.generate();

        TQrPaymentEntity qrPayment = TQrPaymentEntity.builder()
                .userId(userId)
                .machineId(request.getMachineId())
                .amountExpected(request.getAmount())
                .referenceCode(referenceCode)
                .status(QrPaymentStatusEnum.PENDING)
                .build();

        qrPayment = qrPaymentRepository.save(qrPayment);

        /**
         * qrContent: nội dung chuyển khoản gợi ý cho user
         * Format chuẩn VietQR hoặc text hướng dẫn
         */
        String qrContent = AppConstant.QR_REFERENCE_PREFIX + " " + referenceCode;

        return QrPaymentResponse.builder()
                .id(qrPayment.getId())
                .amountExpected(qrPayment.getAmountExpected())
                .referenceCode(referenceCode)
                .status(qrPayment.getStatus())
                .expiredAt(qrPayment.getExpiredAt())
                .qrContent(qrContent)
                // qrImageBase64: tích hợp thư viện ZXing để generate sau
                .build();
    }

    @Override
    @Transactional
    public void processWebhook(WebhookPaymentRequest request) {
        String content = request.getTransferContent();
        if (content == null || content.isBlank()) {
            log.warn("Webhook received with empty transfer content");
            return;
        }

        // Tìm referenceCode trong nội dung chuyển khoản
        // Ví dụ content: "chuyen tien NETCOFFEE NC123456 cam on"
        String referenceCode = ReferenceCodeUtil.extractFromContent(content);
        if (referenceCode == null) {
            log.info("No matching reference code in content: {}", content);
            return;
        }

        TQrPaymentEntity qrPayment = qrPaymentRepository.findByReferenceCode(referenceCode)
                .orElse(null);

        if (qrPayment == null) {
            log.warn("No QR payment found for reference code: {}", referenceCode);
            return;
        }

        if (qrPayment.getStatus() != QrPaymentStatusEnum.PENDING) {
            log.warn("QR payment {} already processed with status {}", referenceCode, qrPayment.getStatus());
            return;
        }

        if (qrPayment.getExpiredAt().isBefore(LocalDateTime.now())) {
            qrPayment.setStatus(QrPaymentStatusEnum.EXPIRED);
            qrPaymentRepository.save(qrPayment);
            log.warn("QR payment {} expired", referenceCode);
            return;
        }

        // Match thành công — cộng tiền
        qrPayment.setStatus(QrPaymentStatusEnum.MATCHED);
        qrPayment.setAmountReceived(request.getTransferAmount());
        qrPayment.setMatchedAt(LocalDateTime.now());
        qrPaymentRepository.save(qrPayment);

        userService.topUp(qrPayment.getUserId(), request.getTransferAmount());

        transactionService.recordTopUp(
                qrPayment.getUserId(),
                request.getTransferAmount(),
                PaymentMethodEnum.QR_BANK,
                request.getTransactionId()
        );

        log.info("QR payment matched: user={}, amount={}, ref={}",
                qrPayment.getUserId(), request.getTransferAmount(), referenceCode);
    }

    @Override
    @Scheduled(fixedRate = 60_000) // Chạy mỗi phút
    @Transactional
    public void expireOldQrPayments() {
        int count = qrPaymentRepository.expireOldQrPayments(LocalDateTime.now());
        if (count > 0) {
            log.info("Expired {} QR payments", count);
        }
    }
}
