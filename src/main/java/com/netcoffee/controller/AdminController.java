package com.netcoffee.controller;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.AdminDeductRequest;
import com.netcoffee.dto.request.CreateCustomerRequest;
import com.netcoffee.dto.request.ResetPasswordRequest;
import com.netcoffee.dto.request.UpdateUserRequest;
import com.netcoffee.dto.response.*;
import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.entity.TSessionEntity;
import com.netcoffee.entity.TTransactionEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.repository.TransactionRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.UserManagementService;
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
public class AdminController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserManagementService userManagementService;
    private final TransactionRepository transactionRepository;
    private final SessionRepository sessionRepository;
    private final MachineRepository machineRepository;

    // -------------------------------------------------------------------------
    // Member management
    // -------------------------------------------------------------------------

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam(defaultValue = "") String phone) {
        List<TUserEntity> users = phone.isBlank()
                ? userRepository.findAllExcludingRole(UserRoleEnum.ADMIN)
                : userRepository.findByPhoneContainingExcludingRole(phone, UserRoleEnum.ADMIN);
        List<UserResponse> result = users.stream().map(userMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo tài khoản hội viên thành công",
                        userManagementService.createCustomer(request)));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công",
                userManagementService.adminUpdateUser(id, request)));
    }

    /**
     * Admin trừ tiền thủ công — chỉ dùng khi nạp nhầm.
     * Ghi transaction DEDUCT với lý do và performedByUserId để audit.
     */
    @PostMapping("/users/{id}/deduct")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> deductBalance(
            @PathVariable Long id,
            @Valid @RequestBody AdminDeductRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        Long adminId = Long.parseLong(userDetails.getUsername());
        TUserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (request.getAmount().compareTo(user.getBalance()) > 0) {
            throw new IllegalArgumentException(
                    "Số dư không đủ để trừ. Hiện có: " + user.getBalance().toPlainString() + "đ");
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(request.getAmount());

        TTransactionEntity tx = TTransactionEntity.builder()
                .userId(id)
                .type(TransactionTypeEnum.DEDUCT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description("Admin trừ tiền: " + request.getReason())
                .paymentMethod(PaymentMethodEnum.ADMIN)
                .performedByUserId(adminId)
                .build();
        transactionRepository.save(tx);
        userService.deduct(id, request.getAmount());

        UserResponse updated = userMapper.toResponse(
                userRepository.findById(id).orElseThrow());
        return ResponseEntity.ok(ApiResponse.ok("Trừ tiền thành công", updated));
    }

    @PutMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        userManagementService.adminResetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công", null));
    }

    // -------------------------------------------------------------------------
    // 30-day transaction history (admin view — includes performedBy)
    // -------------------------------------------------------------------------

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<AdminTransactionResponse>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstant.DEFAULT_PAGE_SIZE) int size) {

        size = Math.min(size, AppConstant.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);

        Page<TTransactionEntity> txPage = transactionRepository.findAllSince(from, pageable);

        // Collect all user IDs: owners + performers
        Set<Long> userIds = new HashSet<>();
        txPage.getContent().forEach(tx -> {
            userIds.add(tx.getUserId());
            if (tx.getPerformedByUserId() != null) userIds.add(tx.getPerformedByUserId());
        });
        Map<Long, TUserEntity> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(TUserEntity::getId, u -> u));

        List<AdminTransactionResponse> content = txPage.getContent().stream().map(tx -> {
            TUserEntity owner    = userMap.get(tx.getUserId());
            TUserEntity performer = tx.getPerformedByUserId() != null ? userMap.get(tx.getPerformedByUserId()) : null;
            return AdminTransactionResponse.builder()
                    .id(tx.getId())
                    .userId(tx.getUserId())
                    .phoneNumber(owner != null ? owner.getPhoneNumber() : null)
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
                    .performedByPhone(performer != null ? performer.getPhoneNumber() : null)
                    .performedByName(performer != null ? performer.getFullName() : null)
                    .createdAt(tx.getCreatedAt())
                    .build();
        }).toList();

        return ResponseEntity.ok(ApiResponse.ok(new PageImpl<>(content, pageable, txPage.getTotalElements())));
    }

    // -------------------------------------------------------------------------
    // 30-day session history
    // -------------------------------------------------------------------------

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('ADMIN')")
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

        return ResponseEntity.ok(ApiResponse.ok(new PageImpl<>(content, pageable, sessionPage.getTotalElements())));
    }
}
