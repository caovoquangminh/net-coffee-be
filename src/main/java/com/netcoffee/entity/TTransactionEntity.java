package com.netcoffee.entity;

import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.*;

@Entity
@Table(
        name = "transactions",
        indexes = {
            @Index(name = "idx_transactions_user_id", columnList = "user_id"),
            @Index(name = "idx_transactions_session_id", columnList = "session_id"),
            @Index(name = "idx_transactions_type", columnList = "type"),
            @Index(name = "idx_transactions_created_at", columnList = "created_at"),
            @Index(name = "idx_transactions_reference_code", columnList = "reference_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionTypeEnum type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Audit trail: lưu balance trước và sau để dễ debug, không cần tính ngược. Không bao giờ derive
     * từ các transaction khác.
     */
    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethodEnum paymentMethod;

    /**
     * Mã tham chiếu từ ngân hàng, dùng để match webhook. Unique constraint để tránh duplicate
     * processing.
     */
    @Column(name = "reference_code", length = 100, unique = true)
    private String referenceCode;

    @Column(name = "session_id")
    private Long sessionId;

    /** Nhân viên/admin thực hiện — null nếu là giao dịch tự động (billing, QR). */
    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
