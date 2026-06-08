package com.netcoffee.service.impl;

import com.netcoffee.dto.response.TransactionResponse;
import com.netcoffee.entity.TTransactionEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import com.netcoffee.mapper.TransactionMapper;
import com.netcoffee.repository.TransactionRepository;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumDeductForSession(Long sessionId) {
        return transactionRepository.sumDeductForSession(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByUserId(Long userId, Pageable pageable) {
        return transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    @Transactional
    public TTransactionEntity recordTopUp(
            Long userId, BigDecimal amount, PaymentMethodEnum method, String referenceCode) {
        return recordTopUp(userId, amount, method, referenceCode, "Nạp tiền");
    }

    @Override
    @Transactional
    public TTransactionEntity recordTopUp(
            Long userId,
            BigDecimal amount,
            PaymentMethodEnum method,
            String referenceCode,
            String description) {
        return recordTopUp(userId, amount, method, referenceCode, description, null);
    }

    @Override
    @Transactional
    public TTransactionEntity recordTopUp(
            Long userId,
            BigDecimal amount,
            PaymentMethodEnum method,
            String referenceCode,
            String description,
            Long performedByUserId) {
        TUserEntity user = userService.getEntityById(userId);

        // increaseBalance ran before this call — back-calculate balanceBefore
        TTransactionEntity tx =
                TTransactionEntity.builder()
                        .userId(userId)
                        .type(TransactionTypeEnum.TOPUP)
                        .amount(amount)
                        .balanceBefore(user.getBalance().subtract(amount))
                        .balanceAfter(user.getBalance())
                        .description(description)
                        .paymentMethod(method)
                        .referenceCode(referenceCode)
                        .performedByUserId(performedByUserId)
                        .build();

        return transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public TTransactionEntity recordDeduct(
            Long userId, BigDecimal amount, Long sessionId, String description) {
        return recordDeduct(userId, amount, sessionId, description, null, null);
    }

    @Override
    @Transactional
    public TTransactionEntity recordDeduct(
            Long userId,
            BigDecimal amount,
            Long sessionId,
            String description,
            PaymentMethodEnum method,
            Long performedByUserId) {
        TUserEntity user = userService.getEntityById(userId);

        // decreaseBalance ran before this call — back-calculate balanceBefore
        TTransactionEntity tx =
                TTransactionEntity.builder()
                        .userId(userId)
                        .type(TransactionTypeEnum.DEDUCT)
                        .amount(amount)
                        .balanceBefore(user.getBalance().add(amount))
                        .balanceAfter(user.getBalance())
                        .description(description)
                        .sessionId(sessionId)
                        .paymentMethod(method)
                        .performedByUserId(performedByUserId)
                        .build();

        return transactionRepository.save(tx);
    }
}
