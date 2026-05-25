package com.netcoffee.controller;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.AdminTopUpRequest;
import com.netcoffee.dto.response.*;
import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.entity.TTransactionEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.repository.TransactionRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.TransactionService;
import com.netcoffee.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final SessionRepository sessionRepository;
    private final MachineRepository machineRepository;

    // -------------------------------------------------------------------------
    // User management
    // -------------------------------------------------------------------------

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam(defaultValue = "") String phone) {
        List<TUserEntity> users = phone.isBlank()
                ? userRepository.findAll()
                : userRepository.findByPhoneNumberContainingOrderByCreatedAtDesc(phone);
        List<UserResponse> result = users.stream().map(userMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    // -------------------------------------------------------------------------
    // Cash top-up
    // -------------------------------------------------------------------------

    @PostMapping("/users/{id}/topup")
    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> cashTopUp(
            @PathVariable Long id,
            @Valid @RequestBody AdminTopUpRequest request) {

        userService.topUp(id, request.getAmount());

        String description = "Nạp tiền mặt" + (request.getNote() != null && !request.getNote().isBlank()
                ? " - " + request.getNote() : "");
        transactionService.recordTopUp(id, request.getAmount(), PaymentMethodEnum.CASH, null, description);

        UserResponse updated = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok("Nạp tiền thành công", updated));
    }

    // -------------------------------------------------------------------------
    // 30-day transaction history
    // -------------------------------------------------------------------------

    @GetMapping("/transactions")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<AdminTransactionResponse>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstant.DEFAULT_PAGE_SIZE) int size) {

        size = Math.min(size, AppConstant.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);

        Page<TTransactionEntity> txPage = transactionRepository.findAllSince(from, pageable);

        Set<Long> userIds = txPage.getContent().stream()
                .map(TTransactionEntity::getUserId).collect(Collectors.toSet());
        Map<Long, TUserEntity> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(TUserEntity::getId, u -> u));

        List<AdminTransactionResponse> content = txPage.getContent().stream().map(tx -> {
            TUserEntity u = userMap.get(tx.getUserId());
            return AdminTransactionResponse.builder()
                    .id(tx.getId())
                    .userId(tx.getUserId())
                    .phoneNumber(u != null ? u.getPhoneNumber() : null)
                    .fullName(u != null ? u.getFullName() : null)
                    .type(tx.getType())
                    .amount(tx.getAmount())
                    .balanceBefore(tx.getBalanceBefore())
                    .balanceAfter(tx.getBalanceAfter())
                    .description(tx.getDescription())
                    .paymentMethod(tx.getPaymentMethod())
                    .referenceCode(tx.getReferenceCode())
                    .sessionId(tx.getSessionId())
                    .createdAt(tx.getCreatedAt())
                    .build();
        }).toList();

        Page<AdminTransactionResponse> result = new PageImpl<>(content, pageable, txPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // -------------------------------------------------------------------------
    // 30-day session history
    // -------------------------------------------------------------------------

    @GetMapping("/sessions")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<SessionHistoryResponse>>> getSessionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstant.DEFAULT_PAGE_SIZE) int size) {

        size = Math.min(size, AppConstant.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);

        Page<TSessionEntity> sessionPage = sessionRepository.findEndedSince(from, pageable);

        Set<Long> userIds = sessionPage.getContent().stream()
                .map(TSessionEntity::getUserId).collect(Collectors.toSet());
        Set<Long> machineIds = sessionPage.getContent().stream()
                .map(TSessionEntity::getMachineId).collect(Collectors.toSet());

        Map<Long, TUserEntity> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(TUserEntity::getId, u -> u));
        Map<Long, TMachineEntity> machineMap = machineRepository.findAllById(machineIds)
                .stream().collect(Collectors.toMap(TMachineEntity::getId, m -> m));

        List<SessionHistoryResponse> content = sessionPage.getContent().stream().map(s -> {
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
        }).toList();

        Page<SessionHistoryResponse> result = new PageImpl<>(content, pageable, sessionPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
