package com.netcoffee.service.impl;

import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.service.CashTopUpService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashTopUpServiceImpl implements CashTopUpService {

    private final UserService userService;
    private final TransactionService transactionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public UserResponse topUp(Long targetUserId, BigDecimal amount, String note, Long performedById) {
        userService.topUp(targetUserId, amount);

        String description = "Nạp tiền mặt" +
                (note != null && !note.isBlank() ? " - " + note.trim() : "");

        transactionService.recordTopUp(
                targetUserId, amount, PaymentMethodEnum.CASH, null,
                description, performedById
        );

        log.info("Cash top-up: target={}, amount={}, performedBy={}", targetUserId, amount, performedById);

        UserResponse updated = userService.findById(targetUserId);
        messagingTemplate.convertAndSend(
                "/topic/balance/" + targetUserId,
                Map.of("balance", updated.getBalance())
        );
        return updated;
    }
}
