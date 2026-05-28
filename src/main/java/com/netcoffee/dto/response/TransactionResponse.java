package com.netcoffee.dto.response;

import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponse {

    private Long id;
    private TransactionTypeEnum type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private PaymentMethodEnum paymentMethod;
    private String referenceCode;
    private Long sessionId;
    private Long performedByUserId;
    private LocalDateTime createdAt;
}
