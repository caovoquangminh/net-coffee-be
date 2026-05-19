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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrPaymentServiceImpl implements QrPaymentService
{

    private final QrPaymentRepository qrPaymentRepository;
    private final UserService userService;
    private final TransactionService transactionService;

    @Value("${app.vietqr.bank-bin}")
    private String bankBin;

    @Value("${app.vietqr.bank-name}")
    private String bankName;

    @Value("${app.vietqr.account-number}")
    private String accountNumber;

    @Value("${app.vietqr.account-name}")
    private String accountName;

    @Override
    @Transactional
    public QrPaymentResponse generateQr(Long userId, TopUpRequest request)
    {
        // Validate user tồn tại
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

        String qrImageUrl = buildVietQrImageUrl(
                request.getAmount().toPlainString(),
                referenceCode
        );

        String qrContent = String.format(
                "%s | %s | %s",
                bankName,
                accountNumber,
                referenceCode
        );

        return QrPaymentResponse.builder()
                .id(qrPayment.getId())
                .amountExpected(qrPayment.getAmountExpected())
                .referenceCode(referenceCode)
                .status(qrPayment.getStatus())
                .expiredAt(qrPayment.getExpiredAt())
                .qrContent(qrContent)
                .qrImageUrl(qrImageUrl)
                .build();
    }

    /**
     * Generate VietQR image URL
     *
     * Example:
     * https://img.vietqr.io/image/970436-1022653510-compact2.png
     * ?amount=50000
     * &addInfo=NC123ABC
     * &accountName=CAO%20VO%20QUANG%20MINH
     */
    private String buildVietQrImageUrl(
            String amount,
            String referenceCode
    )
    {
        String encodedAccountName = URLEncoder.encode(
                accountName,
                StandardCharsets.UTF_8
        );

        String encodedReference = URLEncoder.encode(
                referenceCode,
                StandardCharsets.UTF_8
        );

        return String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                bankBin,
                accountNumber,
                amount,
                encodedReference,
                encodedAccountName
        );
    }

    @Override
    @Transactional
    public void processWebhook(WebhookPaymentRequest request)
    {
        String content = request.getTransferContent();

        if (content == null || content.isBlank()) {
            log.warn("Webhook received with empty transfer content");
            return;
        }

        // Parse reference code từ nội dung chuyển khoản
        String referenceCode = ReferenceCodeUtil.extractFromContent(content);

        if (referenceCode == null) {
            log.info("No matching reference code in content: {}", content);
            return;
        }

        TQrPaymentEntity qrPayment = qrPaymentRepository
                .findByReferenceCode(referenceCode)
                .orElse(null);

        if (qrPayment == null) {
            log.warn("No QR payment found for reference code: {}", referenceCode);
            return;
        }

        // Đã xử lý rồi
        if (qrPayment.getStatus() != QrPaymentStatusEnum.PENDING) {
            log.warn(
                    "QR payment {} already processed with status {}",
                    referenceCode,
                    qrPayment.getStatus()
            );
            return;
        }

        // Hết hạn
        if (qrPayment.getExpiredAt().isBefore(LocalDateTime.now())) {
            qrPayment.setStatus(QrPaymentStatusEnum.EXPIRED);

            qrPaymentRepository.save(qrPayment);

            log.warn("QR payment {} expired", referenceCode);
            return;
        }

        // Match thành công
        qrPayment.setStatus(QrPaymentStatusEnum.MATCHED);
        qrPayment.setAmountReceived(request.getTransferAmount());
        qrPayment.setMatchedAt(LocalDateTime.now());

        qrPaymentRepository.save(qrPayment);

        // Cộng tiền user
        userService.topUp(
                qrPayment.getUserId(),
                request.getTransferAmount()
        );

        // Ghi transaction
        transactionService.recordTopUp(
                qrPayment.getUserId(),
                request.getTransferAmount(),
                PaymentMethodEnum.QR_BANK,
                request.getTransactionId()
        );

        log.info(
                "QR payment matched: user={}, amount={}, ref={}",
                qrPayment.getUserId(),
                request.getTransferAmount(),
                referenceCode
        );
    }

    @Override
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireOldQrPayments()
    {
        int count = qrPaymentRepository.expireOldQrPayments(
                LocalDateTime.now()
        );

        if (count > 0) {
            log.info("Expired {} QR payments", count);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QrPaymentStatusEnum getStatus(String referenceCode)
    {
        return qrPaymentRepository
                .findByReferenceCode(referenceCode)
                .map(TQrPaymentEntity::getStatus)
                .orElse(QrPaymentStatusEnum.EXPIRED);
    }
}