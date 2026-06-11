package com.netcoffee.service.impl;

import com.netcoffee.dto.response.OvertimeRequestResponse;
import com.netcoffee.entity.TOvertimeRequestEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.entity.TWorkShiftEntity;
import com.netcoffee.enumtype.OvertimeStatusEnum;
import com.netcoffee.enumtype.OvertimeTypeEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.OvertimeRequestRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.repository.WorkShiftRepository;
import com.netcoffee.service.OvertimeService;
import com.netcoffee.service.TelegramService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OvertimeServiceImpl implements OvertimeService {

    private final OvertimeRequestRepository overtimeRequestRepository;
    private final WorkShiftRepository workShiftRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;

    @Override
    @Transactional
    public OvertimeRequestResponse createOvertimeRequest(
            Long requesterId,
            Long shiftId,
            String reason,
            OvertimeTypeEnum type,
            Long coveringUserId,
            java.time.LocalDateTime otStart,
            java.time.LocalDateTime otEnd,
            Long replacementUserId) {

        TWorkShiftEntity shift = findShiftOrThrow(shiftId);
        TUserEntity requester = findUserOrThrow(requesterId);

        if (shift.getEndTime()
                .isBefore(
                        java.time.LocalDateTime.now(com.netcoffee.constant.AppConstant.VN_ZONE))) {
            throw new IllegalArgumentException("Không thể tạo OT cho ca đã kết thúc.");
        }

        boolean alreadyExists =
                overtimeRequestRepository.findByRequesterIdAndShiftId(requesterId, shiftId).stream()
                        .anyMatch(
                                r ->
                                        r.getStatus() == OvertimeStatusEnum.PENDING
                                                || r.getStatus() == OvertimeStatusEnum.APPROVED);
        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Bạn đã có yêu cầu OT PENDING hoặc APPROVED cho ca này rồi.");
        }

        TOvertimeRequestEntity request =
                TOvertimeRequestEntity.builder()
                        .requesterId(requesterId)
                        .shiftId(shiftId)
                        .reason(reason)
                        .otType(type)
                        .coveringUserId(coveringUserId)
                        .replacementUserId(replacementUserId)
                        .otStartTime(otStart)
                        .otEndTime(otEnd)
                        .status(OvertimeStatusEnum.PENDING)
                        .build();
        TOvertimeRequestEntity saved = overtimeRequestRepository.save(request);

        String shiftInfo =
                "Ca "
                        + shift.getShiftNumber()
                        + " ngày "
                        + shift.getShiftDate()
                        + " ("
                        + shift.getStartTime()
                        + " - "
                        + shift.getEndTime()
                        + ")";
        try {
            String messageId =
                    telegramService.sendOvertimeRequest(saved, requester.getFullName(), shiftInfo);
            if (messageId != null) {
                saved.setTelegramMessageId(messageId);
                overtimeRequestRepository.save(saved);
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to send Telegram message for OT request {}: {}",
                    saved.getId(),
                    e.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public OvertimeRequestResponse approveOvertime(Long requestId, Long approvedBy) {
        TOvertimeRequestEntity request = findRequestOrThrow(requestId);
        if (request.getStatus() != OvertimeStatusEnum.PENDING) {
            throw new IllegalArgumentException("Yêu cầu OT không ở trạng thái PENDING");
        }

        request.setStatus(OvertimeStatusEnum.APPROVED);
        overtimeRequestRepository.save(request);

        return toResponse(request);
    }

    @Override
    @Transactional
    public OvertimeRequestResponse rejectOvertime(Long requestId) {
        TOvertimeRequestEntity request = findRequestOrThrow(requestId);
        if (request.getStatus() != OvertimeStatusEnum.PENDING) {
            throw new IllegalArgumentException("Yêu cầu OT không ở trạng thái PENDING");
        }
        request.setStatus(OvertimeStatusEnum.REJECTED);
        overtimeRequestRepository.save(request);
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OvertimeRequestResponse> getOvertimeRequests(Long userId, boolean isAdmin) {
        List<TOvertimeRequestEntity> requests;
        if (isAdmin) {
            requests = overtimeRequestRepository.findAll();
        } else {
            requests = overtimeRequestRepository.findByRequesterId(userId);
        }

        Set<Long> userIds =
                requests.stream()
                        .flatMap(
                                r -> {
                                    Stream.Builder<Long> b = Stream.builder();
                                    b.add(r.getRequesterId());
                                    if (r.getCoveringUserId() != null) b.add(r.getCoveringUserId());
                                    return b.build();
                                })
                        .collect(Collectors.toSet());
        Set<Long> shiftIds =
                requests.stream()
                        .map(TOvertimeRequestEntity::getShiftId)
                        .collect(Collectors.toSet());

        Map<Long, TUserEntity> userMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));
        Map<Long, TWorkShiftEntity> shiftMap =
                workShiftRepository.findAllById(shiftIds).stream()
                        .collect(Collectors.toMap(TWorkShiftEntity::getId, s -> s));

        return requests.stream().map(r -> toResponse(r, userMap, shiftMap)).toList();
    }

    private TOvertimeRequestEntity findRequestOrThrow(Long id) {
        return overtimeRequestRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Yêu cầu OT không tồn tại: " + id));
    }

    private TWorkShiftEntity findShiftOrThrow(Long shiftId) {
        return workShiftRepository
                .findById(shiftId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Ca làm việc không tồn tại: " + shiftId));
    }

    private TUserEntity findUserOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Người dùng không tồn tại: " + userId));
    }

    private OvertimeRequestResponse toResponse(TOvertimeRequestEntity r) {
        return OvertimeRequestResponse.builder()
                .id(r.getId())
                .requesterId(r.getRequesterId())
                .shiftId(r.getShiftId())
                .reason(r.getReason())
                .otType(r.getOtType())
                .coveringUserId(r.getCoveringUserId())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private OvertimeRequestResponse toResponse(
            TOvertimeRequestEntity r,
            Map<Long, TUserEntity> userMap,
            Map<Long, TWorkShiftEntity> shiftMap) {
        TUserEntity requester = userMap.get(r.getRequesterId());
        TUserEntity covering =
                r.getCoveringUserId() != null ? userMap.get(r.getCoveringUserId()) : null;
        TWorkShiftEntity shift = shiftMap.get(r.getShiftId());
        return OvertimeRequestResponse.builder()
                .id(r.getId())
                .requesterId(r.getRequesterId())
                .requesterName(requester != null ? requester.getFullName() : null)
                .shiftId(r.getShiftId())
                .shiftNumber(shift != null ? shift.getShiftNumber() : null)
                .shiftDate(shift != null ? shift.getStartTime() : null)
                .reason(r.getReason())
                .otType(r.getOtType())
                .coveringUserId(r.getCoveringUserId())
                .coveringUserName(covering != null ? covering.getFullName() : null)
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
