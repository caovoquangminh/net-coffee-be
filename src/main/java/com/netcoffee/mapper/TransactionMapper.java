package com.netcoffee.mapper;

import com.netcoffee.dto.response.TransactionResponse;
import com.netcoffee.entity.TTransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(TTransactionEntity entity) {
        if (entity == null) return null;
        return TransactionResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .balanceBefore(entity.getBalanceBefore())
                .balanceAfter(entity.getBalanceAfter())
                .description(entity.getDescription())
                .paymentMethod(entity.getPaymentMethod())
                .referenceCode(entity.getReferenceCode())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
