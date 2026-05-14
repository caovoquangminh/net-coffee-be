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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByUserId(Long userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    @Transactional
    public TTransactionEntity recordTopUp(Long userId, BigDecimal amount,
                                          PaymentMethodEnum method, String referenceCode) {
        TUserEntity user = userService.getEntityById(userId);

        TTransactionEntity tx = TTransactionEntity.builder()
                .userId(userId)
                .type(TransactionTypeEnum.TOPUP)
                .amount(amount)
                .balanceBefore(user.getBalance())
                .balanceAfter(user.getBalance().add(amount))
                .description("Nạp tiền")
                .paymentMethod(method)
                .referenceCode(referenceCode)
                .build();

        return transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public TTransactionEntity recordDeduct(Long userId, BigDecimal amount,
                                           Long sessionId, String description) {
        TUserEntity user = userService.getEntityById(userId);

        TTransactionEntity tx = TTransactionEntity.builder()
                .userId(userId)
                .type(TransactionTypeEnum.DEDUCT)
                .amount(amount)
                .balanceBefore(user.getBalance())
                .balanceAfter(user.getBalance().subtract(amount))
                .description(description)
                .sessionId(sessionId)
                .build();

        return transactionRepository.save(tx);
    }
}
