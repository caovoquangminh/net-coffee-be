package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.response.AttendanceRecordResponse;
import com.netcoffee.dto.response.ShiftResponse;
import com.netcoffee.entity.TAttendanceRecordEntity;
import com.netcoffee.entity.TShiftRegistrationEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.entity.TWorkShiftEntity;
import com.netcoffee.enumtype.AttendanceStatusEnum;
import com.netcoffee.enumtype.ShiftRegistrationStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.AttendanceRecordRepository;
import com.netcoffee.repository.OvertimeRequestRepository;
import com.netcoffee.repository.ShiftRegistrationRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.repository.WorkShiftRepository;
import com.netcoffee.service.ShiftService;
import com.netcoffee.service.TelegramService;
import com.netcoffee.utils.AttendanceCalc;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    // Ca 1: 07:00-15:00, Ca 2: 15:00-23:00, Ca 3: 23:00-07:00 (next day)
    private static final LocalTime[][] SHIFT_TIMES = {
        {LocalTime.of(7, 0), LocalTime.of(15, 0)},
        {LocalTime.of(15, 0), LocalTime.of(23, 0)},
        {LocalTime.of(23, 0), LocalTime.of(7, 0)} // end is next day
    };

    private static final int MAX_APPROVED_PER_SHIFT = 2;
    private static final int CHECK_IN_TOLERANCE_MINUTES = 15;

    private final WorkShiftRepository workShiftRepository;
    private final ShiftRegistrationRepository registrationRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final UserRepository userRepository;
    private final OvertimeRequestRepository overtimeRequestRepository;
    private final TelegramService telegramService;

    @Override
    @Transactional
    public void generateShiftsForDate(LocalDate date) {
        for (int shiftNumber = 1; shiftNumber <= 3; shiftNumber++) {
            if (workShiftRepository.existsByShiftNumberAndShiftDate(shiftNumber, date)) {
                continue; // idempotent
            }
            LocalTime startLocal = SHIFT_TIMES[shiftNumber - 1][0];
            LocalTime endLocal = SHIFT_TIMES[shiftNumber - 1][1];
            LocalDateTime startTime = date.atTime(startLocal);
            LocalDateTime endTime;
            if (shiftNumber == 3) {
                // Shift 3 ends at 6:00 the next morning
                endTime = date.plusDays(1).atTime(endLocal);
            } else {
                endTime = date.atTime(endLocal);
            }
            TWorkShiftEntity shift =
                    TWorkShiftEntity.builder()
                            .shiftNumber(shiftNumber)
                            .shiftDate(date)
                            .startTime(startTime)
                            .endTime(endTime)
                            .build();
            workShiftRepository.save(shift);
        }
        log.info("Generated shifts for date: {}", date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> getShiftsForDateRange(LocalDate from, LocalDate to) {
        List<TWorkShiftEntity> shifts = workShiftRepository.findByShiftDateBetween(from, to);

        Set<Long> shiftIds =
                shifts.stream().map(TWorkShiftEntity::getId).collect(Collectors.toSet());
        Map<Long, List<TShiftRegistrationEntity>> regsByShift =
                registrationRepository.findByShiftIdIn(shiftIds).stream()
                        .collect(Collectors.groupingBy(TShiftRegistrationEntity::getShiftId));

        Set<Long> userIds =
                regsByShift.values().stream()
                        .flatMap(List::stream)
                        .map(TShiftRegistrationEntity::getUserId)
                        .collect(Collectors.toSet());
        Map<Long, TUserEntity> userMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));

        return shifts.stream()
                .map(
                        shift ->
                                toShiftResponse(
                                        shift,
                                        regsByShift.getOrDefault(shift.getId(), List.of()),
                                        userMap))
                .toList();
    }

    @Override
    @Transactional
    public ShiftResponse registerShift(Long userId, Long shiftId) {
        TWorkShiftEntity shift = findShiftOrThrow(shiftId);

        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        if (now.isAfter(shift.getEndTime())) {
            throw new IllegalArgumentException("Ca này đã kết thúc, không thể đăng ký.");
        }

        Optional<TShiftRegistrationEntity> existing =
                registrationRepository.findByShiftIdAndUserId(shiftId, userId);
        if (existing.isPresent()) {
            TShiftRegistrationEntity existingReg = existing.get();
            if (existingReg.getStatus() != ShiftRegistrationStatusEnum.CANCELLED) {
                throw new IllegalArgumentException("Bạn đã đăng ký ca này rồi");
            }
            // Re-register after a previous cancellation
            existingReg.setStatus(ShiftRegistrationStatusEnum.REGISTERED);
            registrationRepository.save(existingReg);
            return buildShiftResponse(shift);
        }

        long approvedCount =
                registrationRepository.countByShiftIdAndStatus(
                        shiftId, ShiftRegistrationStatusEnum.APPROVED);
        if (approvedCount >= MAX_APPROVED_PER_SHIFT) {
            throw new IllegalArgumentException(
                    "Ca này đã đủ người (tối đa " + MAX_APPROVED_PER_SHIFT + ")");
        }

        TShiftRegistrationEntity reg =
                TShiftRegistrationEntity.builder()
                        .shiftId(shiftId)
                        .userId(userId)
                        .status(ShiftRegistrationStatusEnum.REGISTERED)
                        .build();
        registrationRepository.save(reg);

        return buildShiftResponse(shift);
    }

    @Override
    @Transactional
    public ShiftResponse approveRegistration(Long registrationId) {
        TShiftRegistrationEntity reg =
                registrationRepository
                        .findById(registrationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Đăng ký không tồn tại: " + registrationId));

        long approvedCount =
                registrationRepository.countByShiftIdAndStatus(
                        reg.getShiftId(), ShiftRegistrationStatusEnum.APPROVED);
        if (approvedCount >= MAX_APPROVED_PER_SHIFT) {
            throw new IllegalArgumentException(
                    "Ca này đã đủ " + MAX_APPROVED_PER_SHIFT + " người được phê duyệt");
        }

        reg.setStatus(ShiftRegistrationStatusEnum.APPROVED);
        registrationRepository.save(reg);

        return buildShiftResponse(findShiftOrThrow(reg.getShiftId()));
    }

    @Override
    @Transactional
    public void cancelRegistration(Long registrationId, Long userId, boolean isAdmin) {
        TShiftRegistrationEntity reg =
                registrationRepository
                        .findById(registrationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Đăng ký không tồn tại: " + registrationId));

        if (!isAdmin && !reg.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền hủy đăng ký của người khác");
        }
        reg.setStatus(ShiftRegistrationStatusEnum.CANCELLED);
        registrationRepository.save(reg);
    }

    @Override
    @Transactional
    public AttendanceRecordResponse checkIn(Long userId, Long shiftId) {
        TWorkShiftEntity shift = findShiftOrThrow(shiftId);

        // Check-in requires an APPROVED registration or an APPROVED OT request for this shift
        boolean hasApprovedRegistration =
                registrationRepository
                        .findByShiftIdAndUserId(shiftId, userId)
                        .map(r -> r.getStatus() == ShiftRegistrationStatusEnum.APPROVED)
                        .orElse(false);

        boolean hasApprovedOT =
                overtimeRequestRepository.findByRequesterIdAndShiftId(userId, shiftId).stream()
                        .anyMatch(
                                ot ->
                                        ot.getStatus()
                                                == com.netcoffee.enumtype.OvertimeStatusEnum
                                                        .APPROVED);

        if (!hasApprovedRegistration && !hasApprovedOT) {
            boolean hasRegistration =
                    registrationRepository.findByShiftIdAndUserId(shiftId, userId).isPresent();
            if (hasRegistration) {
                throw new IllegalArgumentException(
                        "Đăng ký ca của bạn chưa được phê duyệt. Vui lòng liên hệ quản lý.");
            }
            throw new IllegalArgumentException(
                    "Bạn chưa đăng ký ca này. Vui lòng đăng ký và được phê duyệt trước khi check-in.");
        }

        if (attendanceRepository.findByUserIdAndShiftId(userId, shiftId).isPresent()) {
            throw new IllegalArgumentException("Bạn đã check-in ca này rồi");
        }

        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        long minutesAfterStart = Duration.between(shift.getStartTime(), now).toMinutes();

        // Reject check-in earlier than CHECK_IN_TOLERANCE_MINUTES before shift start
        if (minutesAfterStart < -CHECK_IN_TOLERANCE_MINUTES) {
            long minutesUntilCheckIn = -minutesAfterStart - CHECK_IN_TOLERANCE_MINUTES;
            throw new IllegalArgumentException(
                    "Chưa đến giờ check-in. Ca bắt đầu lúc "
                            + shift.getStartTime().toLocalTime()
                            + " (còn "
                            + minutesUntilCheckIn
                            + " phút nữa mới được check-in).");
        }

        if (now.isAfter(shift.getEndTime())) {
            throw new IllegalArgumentException(
                    "Ca đã kết thúc lúc "
                            + shift.getEndTime().toLocalTime()
                            + ". Không thể check-in sau khi ca kết thúc.");
        }

        AttendanceStatusEnum status =
                minutesAfterStart <= CHECK_IN_TOLERANCE_MINUTES
                        ? AttendanceStatusEnum.ON_TIME
                        : AttendanceStatusEnum.LATE;

        TAttendanceRecordEntity record =
                TAttendanceRecordEntity.builder()
                        .userId(userId)
                        .shiftId(shiftId)
                        .checkInTime(now)
                        .attendStatus(status)
                        .isOvertime(hasApprovedOT && !hasApprovedRegistration)
                        .build();
        TAttendanceRecordEntity saved = attendanceRepository.save(record);

        TUserEntity user = userRepository.findById(userId).orElse(null);
        try {
            String name = user != null ? user.getFullName() : "ID " + userId;
            String statusLabel = status == AttendanceStatusEnum.LATE ? "⚠️ Trễ" : "✅ Đúng giờ";
            telegramService.sendAttendanceNotification(
                    String.format(
                            "🟢 Check-in: %s\nCa %d (%s–%s) · %s",
                            name,
                            shift.getShiftNumber(),
                            shift.getStartTime().toLocalTime(),
                            shift.getEndTime().toLocalTime(),
                            statusLabel));
        } catch (Exception e) {
            log.warn("Failed to send check-in Telegram notification: {}", e.getMessage());
        }

        return toAttendanceResponse(saved, user, shift);
    }

    @Override
    @Transactional
    public AttendanceRecordResponse checkOut(Long userId, Long shiftId) {
        TWorkShiftEntity shift = findShiftOrThrow(shiftId);
        TAttendanceRecordEntity record =
                attendanceRepository
                        .findByUserIdAndShiftId(userId, shiftId)
                        .orElseThrow(() -> new ResourceNotFoundException("Chưa check-in ca này"));

        if (record.getCheckOutTime() != null) {
            throw new IllegalArgumentException("Bạn đã check-out ca này rồi");
        }

        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        // Lưu timestamp check-out THẬT (audit), nhưng tính công theo thời điểm hiệu lực có cap
        record.setCheckOutTime(now);

        boolean hasApprovedOt =
                overtimeRequestRepository.findByRequesterIdAndShiftId(userId, shiftId).stream()
                        .anyMatch(
                                ot ->
                                        ot.getStatus()
                                                == com.netcoffee.enumtype.OvertimeStatusEnum
                                                        .APPROVED);

        // Cap tại cuối ca nếu không có OT duyệt → quên check-out / ở lại quá giờ KHÔNG phồng công
        LocalDateTime effectiveOut =
                AttendanceCalc.effectiveCheckOut(now, shift.getEndTime(), hasApprovedOt);
        BigDecimal hoursWorked = AttendanceCalc.workedHours(record.getCheckInTime(), effectiveOut);
        record.setHoursWorked(hoursWorked);

        // Về sớm hơn (cuối ca − dung sai) → EARLY_LEAVE
        record.setAttendStatus(
                AttendanceCalc.resolveCheckoutStatus(
                        record.getAttendStatus(), now, shift.getEndTime()));

        TAttendanceRecordEntity saved = attendanceRepository.save(record);
        TUserEntity user = userRepository.findById(userId).orElse(null);
        try {
            String name = user != null ? user.getFullName() : "ID " + userId;
            telegramService.sendAttendanceNotification(
                    String.format(
                            "🔴 Check-out: %s\nCa %d · %.1f giờ làm việc",
                            name, shift.getShiftNumber(), hoursWorked.doubleValue()));
        } catch (Exception e) {
            log.warn("Failed to send check-out Telegram notification: {}", e.getMessage());
        }

        return toAttendanceResponse(saved, user, shift);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getAttendanceHistory(
            Long userId, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<TAttendanceRecordEntity> records =
                userId != null
                        ? attendanceRepository.findHistoryByUserId(userId, fromDt, toDt)
                        : attendanceRepository.findHistoryAll(fromDt, toDt);

        Set<Long> userIds =
                records.stream()
                        .map(TAttendanceRecordEntity::getUserId)
                        .collect(Collectors.toSet());
        Set<Long> shiftIds =
                records.stream()
                        .map(TAttendanceRecordEntity::getShiftId)
                        .collect(Collectors.toSet());
        Map<Long, TUserEntity> userMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));
        Map<Long, TWorkShiftEntity> shiftMap =
                workShiftRepository.findAllById(shiftIds).stream()
                        .collect(Collectors.toMap(TWorkShiftEntity::getId, s -> s));

        return records.stream()
                .map(
                        r ->
                                toAttendanceResponse(
                                        r,
                                        userMap.get(r.getUserId()),
                                        shiftMap.get(r.getShiftId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getCurrentOnShift() {
        List<TAttendanceRecordEntity> records =
                attendanceRepository.findByCheckInTimeIsNotNullAndCheckOutTimeIsNull();

        Set<Long> userIds =
                records.stream()
                        .map(TAttendanceRecordEntity::getUserId)
                        .collect(Collectors.toSet());
        Set<Long> shiftIds =
                records.stream()
                        .map(TAttendanceRecordEntity::getShiftId)
                        .collect(Collectors.toSet());
        Map<Long, TUserEntity> userMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));
        Map<Long, TWorkShiftEntity> shiftMap =
                workShiftRepository.findAllById(shiftIds).stream()
                        .collect(Collectors.toMap(TWorkShiftEntity::getId, s -> s));

        return records.stream()
                .map(
                        r ->
                                toAttendanceResponse(
                                        r,
                                        userMap.get(r.getUserId()),
                                        shiftMap.get(r.getShiftId())))
                .toList();
    }

    // Chỉ đối soát ca đã kết thúc tối thiểu 30 phút (chừa thời gian check-out muộn) và trong 48h
    // gần
    // đây (giới hạn phạm vi quét).
    private static final long RECONCILE_GRACE_MINUTES = 30;
    private static final long RECONCILE_LOOKBACK_HOURS = 48;

    @Override
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 15 * 60 * 1000L)
    @Transactional
    public int reconcileEndedShifts() {
        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        LocalDateTime upper = now.minusMinutes(RECONCILE_GRACE_MINUTES);
        LocalDateTime lower = now.minusHours(RECONCILE_LOOKBACK_HOURS);

        List<TWorkShiftEntity> endedShifts = workShiftRepository.findByEndTimeBetween(lower, upper);
        int processed = 0;

        for (TWorkShiftEntity shift : endedShifts) {
            List<TShiftRegistrationEntity> approved =
                    registrationRepository.findByShiftIdAndStatus(
                            shift.getId(), ShiftRegistrationStatusEnum.APPROVED);
            if (approved.isEmpty()) {
                continue;
            }

            Map<Long, TAttendanceRecordEntity> recordByUser =
                    attendanceRepository.findByShiftId(shift.getId()).stream()
                            .collect(Collectors.toMap(TAttendanceRecordEntity::getUserId, r -> r));

            for (TShiftRegistrationEntity reg : approved) {
                TAttendanceRecordEntity rec = recordByUser.get(reg.getUserId());

                if (rec == null) {
                    // Được duyệt ca nhưng không check-in → ABSENT
                    attendanceRepository.save(
                            TAttendanceRecordEntity.builder()
                                    .userId(reg.getUserId())
                                    .shiftId(shift.getId())
                                    .attendStatus(AttendanceStatusEnum.ABSENT)
                                    .hoursWorked(BigDecimal.ZERO)
                                    .isOvertime(false)
                                    .note("Tự động: vắng (được duyệt ca nhưng không check-in)")
                                    .build());
                    processed++;
                } else if (rec.getCheckInTime() != null && rec.getCheckOutTime() == null) {
                    // Quên check-out → chốt giờ tại thời điểm kết thúc ca (không phồng công)
                    boolean hasApprovedOt =
                            overtimeRequestRepository
                                    .findByRequesterIdAndShiftId(reg.getUserId(), shift.getId())
                                    .stream()
                                    .anyMatch(
                                            ot ->
                                                    ot.getStatus()
                                                            == com.netcoffee.enumtype
                                                                    .OvertimeStatusEnum.APPROVED);

                    LocalDateTime effectiveOut =
                            AttendanceCalc.effectiveCheckOut(
                                    shift.getEndTime(), shift.getEndTime(), hasApprovedOt);
                    rec.setCheckOutTime(shift.getEndTime());
                    rec.setHoursWorked(
                            AttendanceCalc.workedHours(rec.getCheckInTime(), effectiveOut));
                    String prefix = rec.getNote() != null ? rec.getNote() + " · " : "";
                    rec.setNote(prefix + "Tự động check-out lúc kết thúc ca (quên check-out)");
                    attendanceRepository.save(rec);
                    processed++;
                }
            }
        }

        if (processed > 0) {
            log.info(
                    "Đối soát ca đã kết thúc: xử lý {} bản ghi (auto check-out + ABSENT)",
                    processed);
        }
        return processed;
    }

    private TWorkShiftEntity findShiftOrThrow(Long shiftId) {
        return workShiftRepository
                .findById(shiftId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Ca làm việc không tồn tại: " + shiftId));
    }

    private ShiftResponse buildShiftResponse(TWorkShiftEntity shift) {
        List<TShiftRegistrationEntity> regs = registrationRepository.findByShiftId(shift.getId());
        Set<Long> userIds =
                regs.stream().map(TShiftRegistrationEntity::getUserId).collect(Collectors.toSet());
        Map<Long, TUserEntity> userMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));
        return toShiftResponse(shift, regs, userMap);
    }

    private ShiftResponse toShiftResponse(
            TWorkShiftEntity shift,
            List<TShiftRegistrationEntity> regs,
            Map<Long, TUserEntity> userMap) {
        List<ShiftResponse.RegistrationInfo> regInfos =
                regs.stream()
                        .map(
                                r -> {
                                    TUserEntity u = userMap.get(r.getUserId());
                                    return ShiftResponse.RegistrationInfo.builder()
                                            .registrationId(r.getId())
                                            .userId(r.getUserId())
                                            .userName(u != null ? u.getFullName() : null)
                                            .status(r.getStatus())
                                            .createdAt(r.getCreatedAt())
                                            .build();
                                })
                        .toList();

        return ShiftResponse.builder()
                .id(shift.getId())
                .shiftNumber(shift.getShiftNumber())
                .shiftDate(shift.getShiftDate())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .registrations(regInfos)
                .build();
    }

    private AttendanceRecordResponse toAttendanceResponse(
            TAttendanceRecordEntity record, TUserEntity user, TWorkShiftEntity shift) {
        return AttendanceRecordResponse.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .userName(user != null ? user.getFullName() : null)
                .shiftId(record.getShiftId())
                .shiftNumber(shift != null ? shift.getShiftNumber() : null)
                .shiftDate(shift != null ? shift.getStartTime() : null)
                .checkInTime(record.getCheckInTime())
                .checkOutTime(record.getCheckOutTime())
                .attendStatus(record.getAttendStatus())
                .hoursWorked(record.getHoursWorked())
                .isOvertime(record.getIsOvertime())
                .note(record.getNote())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
