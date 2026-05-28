package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.AdminDeductRequest;
import com.netcoffee.dto.response.AdminTransactionResponse;
import com.netcoffee.dto.response.SessionHistoryResponse;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.entity.TTransactionEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.repository.TransactionRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.AdminService;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final SessionRepository sessionRepository;
    private final MachineRepository machineRepository;

    @Override
    @Transactional
    public UserResponse deductBalance(Long userId, AdminDeductRequest request, Long adminId) {
        TUserEntity user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (request.getAmount().compareTo(user.getBalance()) > 0) {
            throw new IllegalArgumentException(
                    "Số dư không đủ để trừ. Hiện có: " + user.getBalance().toPlainString() + "đ");
        }

        transactionService.recordDeduct(
                userId,
                request.getAmount(),
                null,
                "Admin trừ tiền: " + request.getReason(),
                PaymentMethodEnum.ADMIN,
                adminId);

        userService.deduct(userId, request.getAmount());
        return userMapper.toResponse(userRepository.findById(userId).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> getTransactions(int page, int size) {
        size = Math.min(size, AppConstant.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);

        Page<TTransactionEntity> txPage = transactionRepository.findAllSince(from, pageable);

        Set<Long> userIds = new HashSet<>();
        txPage.getContent()
                .forEach(
                        tx -> {
                            userIds.add(tx.getUserId());
                            if (tx.getPerformedByUserId() != null)
                                userIds.add(tx.getPerformedByUserId());
                        });
        Map<Long, TUserEntity> userMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));

        List<AdminTransactionResponse> content =
                txPage.getContent().stream()
                        .map(
                                tx -> {
                                    TUserEntity owner = userMap.get(tx.getUserId());
                                    TUserEntity performer =
                                            tx.getPerformedByUserId() != null
                                                    ? userMap.get(tx.getPerformedByUserId())
                                                    : null;
                                    return AdminTransactionResponse.builder()
                                            .id(tx.getId())
                                            .userId(tx.getUserId())
                                            .phoneNumber(
                                                    owner != null ? owner.getPhoneNumber() : null)
                                            .fullName(owner != null ? owner.getFullName() : null)
                                            .type(tx.getType())
                                            .amount(tx.getAmount())
                                            .balanceBefore(tx.getBalanceBefore())
                                            .balanceAfter(tx.getBalanceAfter())
                                            .description(tx.getDescription())
                                            .paymentMethod(tx.getPaymentMethod())
                                            .referenceCode(tx.getReferenceCode())
                                            .sessionId(tx.getSessionId())
                                            .performedByUserId(tx.getPerformedByUserId())
                                            .performedByPhone(
                                                    performer != null
                                                            ? performer.getPhoneNumber()
                                                            : null)
                                            .performedByName(
                                                    performer != null
                                                            ? performer.getFullName()
                                                            : null)
                                            .createdAt(tx.getCreatedAt())
                                            .build();
                                })
                        .toList();

        return new PageImpl<>(content, pageable, txPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SessionHistoryResponse> getSessionHistory(int page, int size) {
        size = Math.min(size, AppConstant.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);

        Page<TSessionEntity> sessionPage = sessionRepository.findEndedSince(from, pageable);

        Set<Long> userIds =
                sessionPage.getContent().stream()
                        .map(TSessionEntity::getUserId)
                        .collect(Collectors.toSet());
        Set<Long> machineIds =
                sessionPage.getContent().stream()
                        .map(TSessionEntity::getMachineId)
                        .collect(Collectors.toSet());

        Map<Long, TUserEntity> userMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));
        Map<Long, TMachineEntity> machineMap =
                machineRepository.findAllById(machineIds).stream()
                        .collect(Collectors.toMap(TMachineEntity::getId, m -> m));

        List<SessionHistoryResponse> content =
                sessionPage.getContent().stream()
                        .map(
                                s -> {
                                    TUserEntity u = userMap.get(s.getUserId());
                                    TMachineEntity m = machineMap.get(s.getMachineId());
                                    return SessionHistoryResponse.builder()
                                            .id(s.getId())
                                            .userId(s.getUserId())
                                            .phoneNumber(u != null ? u.getPhoneNumber() : null)
                                            .fullName(u != null ? u.getFullName() : null)
                                            .machineId(s.getMachineId())
                                            .machineCode(m != null ? m.getMachineCode() : null)
                                            .machineName(m != null ? m.getMachineName() : null)
                                            .startedAt(s.getStartedAt())
                                            .endedAt(s.getEndedAt())
                                            .durationSeconds(s.getDurationSeconds())
                                            .totalCost(s.getTotalCost())
                                            .status(s.getStatus())
                                            .isFree(s.getIsFree())
                                            .build();
                                })
                        .toList();

        return new PageImpl<>(content, pageable, sessionPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String phone, boolean isAdmin) {
        List<TUserEntity> users;
        if (isAdmin) {
            users =
                    phone.isBlank()
                            ? userRepository.findAllExcludingRole(UserRoleEnum.ADMIN)
                            : userRepository.findByPhoneContainingExcludingRole(
                                    phone, UserRoleEnum.ADMIN);
        } else {
            users =
                    phone.isBlank()
                            ? userRepository.findByRoleOrderByCreatedAtDesc(UserRoleEnum.CUSTOMER)
                            : userRepository.findByRoleAndPhoneNumberContainingOrderByCreatedAtDesc(
                                    UserRoleEnum.CUSTOMER, phone);
        }
        return users.stream().map(userMapper::toResponse).toList();
    }
}
