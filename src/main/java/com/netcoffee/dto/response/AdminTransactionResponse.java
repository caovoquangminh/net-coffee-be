package com.netcoffee.dto.response;

import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminTransactionResponse {

    private Long id;
    private Long userId;
    private String phoneNumber;
    private String fullName;
    private TransactionTypeEnum type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private PaymentMethodEnum paymentMethod;
    private String referenceCode;
    private Long sessionId;
    private Long performedByUserId;
    private String performedByPhone;
    private String performedByName;
    private LocalDateTime createdAt;
}
