package com.netcoffee.service.impl;

import com.netcoffee.dto.response.ShiftSwapResponse;
import com.netcoffee.entity.TShiftRegistrationEntity;
import com.netcoffee.entity.TShiftSwapRequestEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.entity.TWorkShiftEntity;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import com.netcoffee.enumtype.ShiftRegistrationStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.ShiftRegistrationRepository;
import com.netcoffee.repository.ShiftSwapRequestRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.repository.WorkShiftRepository;
import com.netcoffee.service.ShiftSwapService;
import com.netcoffee.service.TelegramService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftSwapServiceImpl implements ShiftSwapService {

    private final ShiftSwapRequestRepository swapRepository;
    private final ShiftRegistrationRepository registrationRepository;
    private final WorkShiftRepository workShiftRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;

    @Override
    @Transactional
    public ShiftSwapResponse create(Long fromUserId, Long toUserId, Long shiftId, String reason) {
        TWorkShiftEntity shift = findShiftOrThrow(shiftId);
        TUserEntity fromUser = findUserOrThrow(fromUserId);
        TUserEntity toUser = findUserOrThrow(toUserId);

        boolean fromHasShift =
                registrationRepository
                        .findByShiftIdAndUserId(shiftId, fromUserId)
                        .map(this::isActive)
                        .orElse(false);
        if (!fromHasShift) {
            throw new IllegalArgumentException("Bạn không có ca này để đổi.");
        }
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Không thể đổi ca cho chính mình.");
        }

        TShiftSwapRequestEntity req =
                swapRepository.save(
                        TShiftSwapRequestEntity.builder()
                                .fromUserId(fromUserId)
                                .toUserId(toUserId)
                                .shiftId(shiftId)
                                .reason(reason)
                                .status(ApprovalStatusEnum.PENDING)
                                .build());

        String text =
                String.format(
                        "🔄 *Yêu cầu đổi ca*\n"
                                + "Người nhường: %s\n"
                                + "Người nhận: %s\n"
                                + "Ca %d ngày %s (%s-%s)\n"
                                + "Lý do: %s\n"
                                + "ID: %d",
                        fromUser.getFullName(),
                        toUser.getFullName(),
                        shift.getShiftNumber(),
                        shift.getShiftDate(),
                        shift.getStartTime().toLocalTime(),
                        shift.getEndTime().toLocalTime(),
                        reason != null ? reason : "Không có",
                        req.getId());
        try {
            String msgId = telegramService.sendApprovalRequest("swap", req.getId(), text);
            if (msgId != null) {
                req.setTelegramMessageId(msgId);
                swapRepository.save(req);
            }
        } catch (Exception e) {
            log.warn("Gửi Telegram đổi ca {} thất bại: {}", req.getId(), e.getMessage());
        }
        return toResponse(req, fromUser, toUser, shift);
    }

    @Override
    @Transactional
    public ShiftSwapResponse approve(Long id) {
        TShiftSwapRequestEntity req = findOrThrow(id);
        if (req.getStatus() != ApprovalStatusEnum.PENDING) {
            throw new IllegalArgumentException("Yêu cầu đổi ca không ở trạng thái chờ duyệt.");
        }

        // Hủy đăng ký của người nhường, gán ca cho người nhận.
        registrationRepository
                .findByShiftIdAndUserId(req.getShiftId(), req.getFromUserId())
                .ifPresent(
                        r -> {
                            r.setStatus(ShiftRegistrationStatusEnum.CANCELLED);
                            registrationRepository.save(r);
                        });

        Optional<TShiftRegistrationEntity> toReg =
                registrationRepository.findByShiftIdAndUserId(req.getShiftId(), req.getToUserId());
        if (toReg.isPresent()) {
            TShiftRegistrationEntity r = toReg.get();
            r.setStatus(ShiftRegistrationStatusEnum.ADMIN_ASSIGNED);
            registrationRepository.save(r);
        } else {
            registrationRepository.save(
                    TShiftRegistrationEntity.builder()
                            .shiftId(req.getShiftId())
                            .userId(req.getToUserId())
                            .status(ShiftRegistrationStatusEnum.ADMIN_ASSIGNED)
                            .build());
        }

        req.setStatus(ApprovalStatusEnum.APPROVED);
        swapRepository.save(req);
        return toResponse(req);
    }

    @Override
    @Transactional
    public ShiftSwapResponse reject(Long id) {
        TShiftSwapRequestEntity req = findOrThrow(id);
        if (req.getStatus() != ApprovalStatusEnum.PENDING) {
            throw new IllegalArgumentException("Yêu cầu đổi ca không ở trạng thái chờ duyệt.");
        }
        req.setStatus(ApprovalStatusEnum.REJECTED);
        swapRepository.save(req);
        return toResponse(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftSwapResponse> list(Long userId, boolean isAdmin) {
        List<TShiftSwapRequestEntity> all = swapRepository.findAll();
        return all.stream()
                .filter(
                        r ->
                                isAdmin
                                        || r.getFromUserId().equals(userId)
                                        || r.getToUserId().equals(userId))
                .map(this::toResponse)
                .toList();
    }

    private boolean isActive(TShiftRegistrationEntity r) {
        return r.getStatus() == ShiftRegistrationStatusEnum.REGISTERED
                || r.getStatus() == ShiftRegistrationStatusEnum.ADMIN_ASSIGNED;
    }

    private TShiftSwapRequestEntity findOrThrow(Long id) {
        return swapRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Yêu cầu đổi ca không tồn tại: " + id));
    }

    private TWorkShiftEntity findShiftOrThrow(Long id) {
        return workShiftRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ca không tồn tại: " + id));
    }

    private TUserEntity findUserOrThrow(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Người dùng không tồn tại: " + id));
    }

    private ShiftSwapResponse toResponse(TShiftSwapRequestEntity r) {
        TUserEntity from = userRepository.findById(r.getFromUserId()).orElse(null);
        TUserEntity to = userRepository.findById(r.getToUserId()).orElse(null);
        TWorkShiftEntity shift = workShiftRepository.findById(r.getShiftId()).orElse(null);
        return toResponse(r, from, to, shift);
    }

    private ShiftSwapResponse toResponse(
            TShiftSwapRequestEntity r, TUserEntity from, TUserEntity to, TWorkShiftEntity shift) {
        return ShiftSwapResponse.builder()
                .id(r.getId())
                .fromUserId(r.getFromUserId())
                .fromUserName(from != null ? from.getFullName() : null)
                .toUserId(r.getToUserId())
                .toUserName(to != null ? to.getFullName() : null)
                .shiftId(r.getShiftId())
                .shiftNumber(shift != null ? shift.getShiftNumber() : null)
                .shiftDate(shift != null ? shift.getStartTime() : null)
                .reason(r.getReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
