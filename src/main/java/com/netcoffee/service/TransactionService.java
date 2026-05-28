package com.netcoffee.service;

import com.netcoffee.dto.response.TransactionResponse;
import com.netcoffee.entity.TTransactionEntity;
import com.netcoffee.enumtype.PaymentMethodEnum;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    Page<TransactionResponse> findByUserId(Long userId, Pageable pageable);

    TTransactionEntity recordTopUp(
            Long userId, BigDecimal amount, PaymentMethodEnum method, String referenceCode);

    TTransactionEntity recordTopUp(
            Long userId,
            BigDecimal amount,
            PaymentMethodEnum method,
            String referenceCode,
            String description);

    TTransactionEntity recordTopUp(
            Long userId,
            BigDecimal amount,
            PaymentMethodEnum method,
            String referenceCode,
            String description,
            Long performedByUserId);

    TTransactionEntity recordDeduct(
            Long userId, BigDecimal amount, Long sessionId, String description);

    TTransactionEntity recordDeduct(
            Long userId,
            BigDecimal amount,
            Long sessionId,
            String description,
            PaymentMethodEnum method,
            Long performedByUserId);
}
