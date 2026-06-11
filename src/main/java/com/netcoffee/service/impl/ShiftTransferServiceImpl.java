package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.response.ShiftTransferResponse;
import com.netcoffee.entity.TShiftAssignmentEntity;
import com.netcoffee.entity.TShiftRegistrationEntity;
import com.netcoffee.entity.TShiftTransferRequestEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.entity.TWorkShiftEntity;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import com.netcoffee.enumtype.AssignmentSourceEnum;
import com.netcoffee.enumtype.ShiftRegistrationStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.ShiftAssignmentRepository;
import com.netcoffee.repository.ShiftRegistrationRepository;
import com.netcoffee.repository.ShiftTransferRequestRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.repository.WorkShiftRepository;
import com.netcoffee.service.ShiftTransferService;
import com.netcoffee.service.TelegramService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftTransferServiceImpl implements ShiftTransferService {

    private final ShiftTransferRequestRepository transferRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final ShiftRegistrationRepository registrationRepository;
    private final WorkShiftRepository workShiftRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;

    @Override
    @Transactional
    public ShiftTransferResponse create(
            Long originalUserId,
            Long shiftId,
            Long replacementUserId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String reason) {
        TWorkShiftEntity shift = findShift(shiftId);
        TUserEntity a = findUser(originalUserId);
        TUserEntity b = findUser(replacementUserId);

        if (originalUserId.equals(replacementUserId)) {
            throw new IllegalArgumentException("Không thể nhờ chính mình làm thay.");
        }
        if (shift.getEndTime().isBefore(LocalDateTime.now(AppConstant.VN_ZONE))) {
            throw new IllegalArgumentException("Ca đã kết thúc, không thể nhờ làm thay.");
        }
        boolean aHasShift =
                registrationRepository
                        .findByShiftIdAndUserId(shiftId, originalUserId)
                        .map(this::isActive)
                        .orElse(false);
        if (!aHasShift) {
            throw new IllegalArgumentException("Bạn không giữ ca này để nhờ làm thay.");
        }
        validateSegment(shift, startTime, endTime);

        TShiftTransferRequestEntity req =
                transferRepository.save(
                        TShiftTransferRequestEntity.builder()
                                .shiftId(shiftId)
                                .originalUserId(originalUserId)
                                .replacementUserId(replacementUserId)
                                .startTime(startTime)
                                .endTime(endTime)
                                .reason(reason)
                                .status(ApprovalStatusEnum.PENDING)
                                .build());

        String text =
                String.format(
                        "🔁 *Làm thay một phần ca*\n"
                                + "Người nhờ: %s\n"
                                + "Người làm thay: %s\n"
                                + "Ca %d ngày %s\n"
                                + "Đoạn làm thay: %s - %s\n"
                                + "Lý do: %s\n"
                                + "ID: %d",
                        a.getFullName(),
                        b.getFullName(),
                        shift.getShiftNumber(),
                        shift.getShiftDate(),
                        startTime.toLocalTime(),
                        endTime.toLocalTime(),
                        reason != null ? reason : "Không có",
                        req.getId());
        try {
            String msgId = telegramService.sendApprovalRequest("transfer", req.getId(), text);
            if (msgId != null) {
                req.setTelegramMessageId(msgId);
                transferRepository.save(req);
            }
        } catch (Exception e) {
            log.warn("Gửi Telegram transfer {} thất bại: {}", req.getId(), e.getMessage());
        }
        return toResponse(req, a, b, shift);
    }

    @Override
    @Transactional
    public ShiftTransferResponse approve(Long id, Long approvedBy) {
        TShiftTransferRequestEntity req = findReq(id);
        if (req.getStatus() != ApprovalStatusEnum.PENDING) {
            throw new IllegalArgumentException("Yêu cầu không ở trạng thái chờ duyệt.");
        }
        TWorkShiftEntity shift = findShift(req.getShiftId());

        // Đoạn còn lại của A = ca trừ đi đoạn B làm thay (B luôn áp một biên của ca).
        LocalDateTime aStart;
        LocalDateTime aEnd;
        if (req.getStartTime().equals(shift.getStartTime())) {
            // B làm đầu ca → A giữ phần cuối
            aStart = req.getEndTime();
            aEnd = shift.getEndTime();
        } else {
            // B làm cuối ca → A giữ phần đầu
            aStart = shift.getStartTime();
            aEnd = req.getStartTime();
        }

        upsertAssignment(
                req.getShiftId(),
                req.getOriginalUserId(),
                aStart,
                aEnd,
                AssignmentSourceEnum.REGISTRATION,
                null);
        upsertAssignment(
                req.getShiftId(),
                req.getReplacementUserId(),
                req.getStartTime(),
                req.getEndTime(),
                AssignmentSourceEnum.TRANSFER,
                req.getId());

        // B phải "có mặt" trong ca → tạo/khôi phục đăng ký ADMIN_ASSIGNED.
        registrationRepository
                .findByShiftIdAndUserId(req.getShiftId(), req.getReplacementUserId())
                .ifPresentOrElse(
                        r -> {
                            r.setStatus(ShiftRegistrationStatusEnum.ADMIN_ASSIGNED);
                            registrationRepository.save(r);
                        },
                        () ->
                                registrationRepository.save(
                                        TShiftRegistrationEntity.builder()
                                                .shiftId(req.getShiftId())
                                                .userId(req.getReplacementUserId())
                                                .status(ShiftRegistrationStatusEnum.ADMIN_ASSIGNED)
                                                .build()));

        req.setStatus(ApprovalStatusEnum.APPROVED);
        req.setApprovedBy(approvedBy);
        req.setApprovedAt(LocalDateTime.now(AppConstant.VN_ZONE));
        transferRepository.save(req);
        return toResponse(req);
    }

    @Override
    @Transactional
    public ShiftTransferResponse reject(Long id) {
        TShiftTransferRequestEntity req = findReq(id);
        if (req.getStatus() != ApprovalStatusEnum.PENDING) {
            throw new IllegalArgumentException("Yêu cầu không ở trạng thái chờ duyệt.");
        }
        req.setStatus(ApprovalStatusEnum.REJECTED);
        transferRepository.save(req);
        return toResponse(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftTransferResponse> list(Long userId, boolean isAdmin) {
        return transferRepository.findAll().stream()
                .filter(
                        r ->
                                isAdmin
                                        || r.getOriginalUserId().equals(userId)
                                        || r.getReplacementUserId().equals(userId))
                .map(this::toResponse)
                .toList();
    }

    // ---- helpers

    private void validateSegment(TWorkShiftEntity shift, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Khoảng giờ làm thay không hợp lệ.");
        }
        if (start.isBefore(shift.getStartTime()) || end.isAfter(shift.getEndTime())) {
            throw new IllegalArgumentException("Đoạn làm thay phải nằm trong giờ ca.");
        }
        boolean touchesBoundary =
                start.equals(shift.getStartTime()) || end.equals(shift.getEndTime());
        boolean wholeShift = start.equals(shift.getStartTime()) && end.equals(shift.getEndTime());
        if (!touchesBoundary || wholeShift) {
            throw new IllegalArgumentException(
                    "Chỉ hỗ trợ làm thay phần ĐẦU hoặc phần CUỐI ca (không phải toàn bộ/giữa ca).");
        }
    }

    private void upsertAssignment(
            Long shiftId,
            Long userId,
            LocalDateTime start,
            LocalDateTime end,
            AssignmentSourceEnum source,
            Long transferRequestId) {
        assignmentRepository
                .findByShiftIdAndUserId(shiftId, userId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setStartTime(start);
                            existing.setEndTime(end);
                            existing.setSource(source);
                            existing.setTransferRequestId(transferRequestId);
                            assignmentRepository.save(existing);
                        },
                        () ->
                                assignmentRepository.save(
                                        TShiftAssignmentEntity.builder()
                                                .shiftId(shiftId)
                                                .userId(userId)
                                                .startTime(start)
                                                .endTime(end)
                                                .source(source)
                                                .transferRequestId(transferRequestId)
                                                .build()));
    }

    private boolean isActive(TShiftRegistrationEntity r) {
        return r.getStatus() == ShiftRegistrationStatusEnum.REGISTERED
                || r.getStatus() == ShiftRegistrationStatusEnum.ADMIN_ASSIGNED;
    }

    private TShiftTransferRequestEntity findReq(Long id) {
        return transferRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại: " + id));
    }

    private TWorkShiftEntity findShift(Long id) {
        return workShiftRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ca không tồn tại: " + id));
    }

    private TUserEntity findUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Người dùng không tồn tại: " + id));
    }

    private ShiftTransferResponse toResponse(TShiftTransferRequestEntity r) {
        TUserEntity a = userRepository.findById(r.getOriginalUserId()).orElse(null);
        TUserEntity b = userRepository.findById(r.getReplacementUserId()).orElse(null);
        TWorkShiftEntity shift = workShiftRepository.findById(r.getShiftId()).orElse(null);
        return toResponse(r, a, b, shift);
    }

    private ShiftTransferResponse toResponse(
            TShiftTransferRequestEntity r, TUserEntity a, TUserEntity b, TWorkShiftEntity shift) {
        return ShiftTransferResponse.builder()
                .id(r.getId())
                .shiftId(r.getShiftId())
                .shiftNumber(shift != null ? shift.getShiftNumber() : null)
                .shiftDate(shift != null ? shift.getStartTime() : null)
                .originalUserId(r.getOriginalUserId())
                .originalUserName(a != null ? a.getFullName() : null)
                .replacementUserId(r.getReplacementUserId())
                .replacementUserName(b != null ? b.getFullName() : null)
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .reason(r.getReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
