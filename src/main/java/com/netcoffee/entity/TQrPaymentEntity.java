package com.netcoffee.entity;

import com.netcoffee.enumtype.QrPaymentStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "qr_payments", indexes = { @Index(name = "idx_qr_payments_user_id", columnList = "user_id"),
        @Index(name = "idx_qr_payments_machine_id", columnList = "machine_id"),
        @Index(name = "idx_qr_payments_status", columnList = "status"),
        /**
         * Index quan trọng nhất: reference_code được query liên tục khi webhook
         * ngân hàng gửi về để match giao dịch
         */
        @Index(name = "idx_qr_payments_reference_code", columnList = "reference_code", unique = true) }) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TQrPaymentEntity
{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "amount_expected", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountExpected;

    /**
     * amountReceived có thể khác amountExpected (user CK nhầm số). Hệ thống sẽ
     * cộng đúng số thực nhận.
     */
    @Column(name = "amount_received", precision = 15, scale = 2)
    private BigDecimal amountReceived;

    /**
     * Mã nhúng vào nội dung chuyển khoản để auto-match. Ví dụ: "NETCOFFEE
     * NC123456"
     */
    @Column(name = "reference_code", nullable = false, unique = true, length = 100)
    private String referenceCode;

    /**
     * Fix: dùng Enum thay String để type-safe
     */
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private QrPaymentStatusEnum status = QrPaymentStatusEnum.PENDING;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
        if (expiredAt == null)
        {
            // Default: QR hết hạn sau 15 phút
            expiredAt = LocalDateTime.now().plusMinutes(15);
        }
    }
}
