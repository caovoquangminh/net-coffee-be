package com.netcoffee.service.impl;

import com.netcoffee.dto.response.LeaveRequestResponse;
import com.netcoffee.entity.TLeaveRequestEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.entity.TWorkShiftEntity;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import com.netcoffee.enumtype.LeaveTypeEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.LeaveRequestRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.repository.WorkShiftRepository;
import com.netcoffee.service.LeaveService;
import com.netcoffee.service.TelegramService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final WorkShiftRepository workShiftRepository;
    private final UserRepository userRepository;
    private final com.netcoffee.repository.ShiftRegistrationRepository registrationRepository;
    private final TelegramService telegramService;

    @Override
    @Transactional
    public LeaveRequestResponse create(
            Long userId, Long shiftId, LocalDate date, LeaveTypeEnum type, String reason) {
        TUserEntity user = findUserOrThrow(userId);
        if (shiftId == null) {
            throw new IllegalArgumentException("Phải chọn ca cụ thể để xin nghỉ.");
        }
        TWorkShiftEntity shift =
                workShiftRepository
                        .findById(shiftId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Ca không tồn tại: " + shiftId));
        if (shift.getEndTime()
                .isBefore(
                        java.time.LocalDateTime.now(com.netcoffee.constant.AppConstant.VN_ZONE))) {
            throw new IllegalArgumentException("Không thể xin nghỉ ca đã qua.");
        }
        boolean registered =
                registrationRepository
                        .findByShiftIdAndUserId(shiftId, userId)
                        .map(
                                r ->
                                        r.getStatus()
                                                        == com.netcoffee.enumtype
                                                                .ShiftRegistrationStatusEnum
                                                                .REGISTERED
                                                || r.getStatus()
                                                        == com.netcoffee.enumtype
                                                                .ShiftRegistrationStatusEnum
                                                                .ADMIN_ASSIGNED)
                        .orElse(false);
        if (!registered) {
            throw new IllegalArgumentException("Bạn chưa đăng ký ca này nên không cần xin nghỉ.");
        }
        date = shift.getShiftDate();

        TLeaveRequestEntity req =
                leaveRepository.save(
                        TLeaveRequestEntity.builder()
                                .userId(userId)
                                .shiftId(shiftId)
                                .leaveDate(date)
                                .leaveType(type)
                                .reason(reason)
                                .status(ApprovalStatusEnum.PENDING)
                                .build());

        String text =
                String.format(
                        "🌴 *Đơn nghỉ phép*\n"
                                + "Nhân viên: %s\n"
                                + "Ngày: %s\n"
                                + "Loại: %s\n"
                                + "Lý do: %s\n"
                                + "ID: %d",
                        user.getFullName(),
                        com.netcoffee.utils.DateFmt.date(date),
                        type != null ? type.name() : "N/A",
                        reason != null ? reason : "Không có",
                        req.getId());
        try {
            String msgId = telegramService.sendApprovalRequest("leave", req.getId(), text);
            if (msgId != null) {
                req.setTelegramMessageId(msgId);
                leaveRepository.save(req);
            }
        } catch (Exception e) {
            log.warn("Gửi Telegram đơn nghỉ {} thất bại: {}", req.getId(), e.getMessage());
        }
        return toResponse(req, user);
    }

    @Override
    @Transactional
    public LeaveRequestResponse approve(Long id) {
        TLeaveRequestEntity req = findOrThrow(id);
        if (req.getStatus() != ApprovalStatusEnum.PENDING) {
            throw new IllegalArgumentException("Đơn nghỉ không ở trạng thái chờ duyệt.");
        }
        req.setStatus(ApprovalStatusEnum.APPROVED);
        leaveRepository.save(req);
        return toResponse(req, null);
    }

    @Override
    @Transactional
    public LeaveRequestResponse reject(Long id) {
        TLeaveRequestEntity req = findOrThrow(id);
        if (req.getStatus() != ApprovalStatusEnum.PENDING) {
            throw new IllegalArgumentException("Đơn nghỉ không ở trạng thái chờ duyệt.");
        }
        req.setStatus(ApprovalStatusEnum.REJECTED);
        leaveRepository.save(req);
        return toResponse(req, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> list(Long userId, boolean isAdmin) {
        List<TLeaveRequestEntity> all =
                isAdmin ? leaveRepository.findAll() : leaveRepository.findByUserId(userId);
        return all.stream().map(r -> toResponse(r, null)).toList();
    }

    private TLeaveRequestEntity findOrThrow(Long id) {
        return leaveRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn nghỉ không tồn tại: " + id));
    }

    private TUserEntity findUserOrThrow(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Người dùng không tồn tại: " + id));
    }

    private LeaveRequestResponse toResponse(TLeaveRequestEntity r, TUserEntity userOrNull) {
        TUserEntity user =
                userOrNull != null
                        ? userOrNull
                        : userRepository.findById(r.getUserId()).orElse(null);
        TWorkShiftEntity shift =
                r.getShiftId() != null
                        ? workShiftRepository.findById(r.getShiftId()).orElse(null)
                        : null;
        return LeaveRequestResponse.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .userName(user != null ? user.getFullName() : null)
                .shiftId(r.getShiftId())
                .shiftNumber(shift != null ? shift.getShiftNumber() : null)
                .leaveDate(r.getLeaveDate())
                .leaveType(r.getLeaveType())
                .reason(r.getReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
