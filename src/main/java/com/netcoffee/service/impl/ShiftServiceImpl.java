package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.response.AttendanceRecordResponse;
import com.netcoffee.dto.response.ShiftResponse;
import com.netcoffee.entity.TAttendanceRecordEntity;
import com.netcoffee.entity.TShiftRegistrationEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.entity.TWorkShiftEntity;
import com.netcoffee.enumtype.ApprovalStatusEnum;
import com.netcoffee.enumtype.AttendanceStatusEnum;
import com.netcoffee.enumtype.OvertimeStatusEnum;
import com.netcoffee.enumtype.ShiftRegistrationStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.AttendanceRecordRepository;
import com.netcoffee.repository.LeaveRequestRepository;
import com.netcoffee.repository.OvertimeRequestRepository;
import com.netcoffee.repository.ShiftRegistrationRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.repository.WorkShiftRepository;
import com.netcoffee.service.ShiftService;
import com.netcoffee.service.TelegramService;
import com.netcoffee.utils.AttendanceCalc;
import com.netcoffee.utils.HandoverRedistribution;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    // Giờ quán net thật: ca1 06:00-11:00, ca2 11:00-17:00, ca3 17:00-22:00 (mở 06:00-22:00).
    private static final LocalTime[][] SHIFT_TIMES = {
        {LocalTime.of(6, 0), LocalTime.of(11, 0)},
        {LocalTime.of(11, 0), LocalTime.of(17, 0)},
        {LocalTime.of(17, 0), LocalTime.of(22, 0)}
    };

    private static final int MAX_PER_SHIFT = 2;

    // Đối soát ca đã kết thúc tối thiểu 30 phút trước (chừa thời gian check-out muộn), trong 48h
    // gần.
    private static final long RECONCILE_GRACE_MINUTES = 30;
    private static final long RECONCILE_LOOKBACK_HOURS = 48;

    private final WorkShiftRepository workShiftRepository;
    private final ShiftRegistrationRepository registrationRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final OvertimeRequestRepository overtimeRequestRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;

    // ====================================================================== shifts

    @Override
    @Transactional
    public void generateShiftsForDate(LocalDate date) {
        for (int shiftNumber = 1; shiftNumber <= 3; shiftNumber++) {
            if (workShiftRepository.existsByShiftNumberAndShiftDate(shiftNumber, date)) {
                continue; // idempotent
            }
            LocalTime startLocal = SHIFT_TIMES[shiftNumber - 1][0];
            LocalTime endLocal = SHIFT_TIMES[shiftNumber - 1][1];
            TWorkShiftEntity shift =
                    TWorkShiftEntity.builder()
                            .shiftNumber(shiftNumber)
                            .shiftDate(date)
                            .startTime(date.atTime(startLocal))
                            .endTime(date.atTime(endLocal))
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
                        s ->
                                toShiftResponse(
                                        s, regsByShift.getOrDefault(s.getId(), List.of()), userMap))
                .toList();
    }

    // ================================================================ registration

    @Override
    public boolean isRegistrationWindowOpen() {
        DayOfWeek d = LocalDate.now(AppConstant.VN_ZONE).getDayOfWeek();
        return d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /** Khoảng tuần kế tiếp (Thứ 2 đến Chủ nhật). */
    private boolean isInNextWeek(LocalDate shiftDate, LocalDate today) {
        LocalDate nextMonday =
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1);
        LocalDate nextSunday = nextMonday.plusDays(6);
        return !shiftDate.isBefore(nextMonday) && !shiftDate.isAfter(nextSunday);
    }

    @Override
    @Transactional
    public ShiftResponse registerShift(Long userId, Long shiftId) {
        TWorkShiftEntity shift = findShiftOrThrow(shiftId);
        LocalDate today = LocalDate.now(AppConstant.VN_ZONE);

        if (!isRegistrationWindowOpen()) {
            throw new IllegalArgumentException(
                    "Chỉ được đăng ký ca vào Thứ 6, Thứ 7 hoặc Chủ nhật (cho tuần kế tiếp).");
        }
        if (!isInNextWeek(shift.getShiftDate(), today)) {
            throw new IllegalArgumentException("Chỉ được đăng ký ca của TUẦN KẾ TIẾP.");
        }
        return doRegister(userId, shift, ShiftRegistrationStatusEnum.REGISTERED);
    }

    @Override
    @Transactional
    public ShiftResponse assignShift(Long shiftId, Long userId) {
        TWorkShiftEntity shift = findShiftOrThrow(shiftId);
        // Admin sắp ca chậm: bỏ qua kiểm tra cửa sổ/tuần.
        return doRegister(userId, shift, ShiftRegistrationStatusEnum.ADMIN_ASSIGNED);
    }

    private ShiftResponse doRegister(
            Long userId, TWorkShiftEntity shift, ShiftRegistrationStatusEnum status) {
        Optional<TShiftRegistrationEntity> existing =
                registrationRepository.findByShiftIdAndUserId(shift.getId(), userId);
        if (existing.isPresent()) {
            TShiftRegistrationEntity reg = existing.get();
            if (reg.getStatus() != ShiftRegistrationStatusEnum.CANCELLED) {
                throw new IllegalArgumentException("Bạn đã đăng ký ca này rồi.");
            }
            reg.setStatus(status);
            registrationRepository.save(reg);
            return buildShiftResponse(shift);
        }

        long active = countActiveRegistrations(shift.getId());
        if (active >= MAX_PER_SHIFT) {
            throw new IllegalArgumentException(
                    "Ca này đã đủ người (tối đa " + MAX_PER_SHIFT + ").");
        }

        registrationRepository.save(
                TShiftRegistrationEntity.builder()
                        .shiftId(shift.getId())
                        .userId(userId)
                        .status(status)
                        .build());
        return buildShiftResponse(shift);
    }

    /** Số đăng ký còn hiệu lực (REGISTERED hoặc ADMIN_ASSIGNED). */
    private long countActiveRegistrations(Long shiftId) {
        return registrationRepository.findByShiftId(shiftId).stream()
                .filter(
                        r ->
                                r.getStatus() == ShiftRegistrationStatusEnum.REGISTERED
                                        || r.getStatus()
                                                == ShiftRegistrationStatusEnum.ADMIN_ASSIGNED)
                .count();
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
                    "Bạn không có quyền hủy đăng ký của người khác.");
        }
        reg.setStatus(ShiftRegistrationStatusEnum.CANCELLED);
        registrationRepository.save(reg);
    }

    // =================================================================== check-in

    private boolean isActiveRegistration(Long shiftId, Long userId) {
        return registrationRepository
                .findByShiftIdAndUserId(shiftId, userId)
                .map(
                        r ->
                                r.getStatus() == ShiftRegistrationStatusEnum.REGISTERED
                                        || r.getStatus()
                                                == ShiftRegistrationStatusEnum.ADMIN_ASSIGNED)
                .orElse(false);
    }

    private boolean hasApprovedOt(Long userId, Long shiftId) {
        return overtimeRequestRepository.findByRequesterIdAndShiftId(userId, shiftId).stream()
                .anyMatch(ot -> ot.getStatus() == OvertimeStatusEnum.APPROVED);
    }

    @Override
    @Transactional
    public AttendanceRecordResponse checkIn(Long userId, Long shiftId) {
        TWorkShiftEntity shift = findShiftOrThrow(shiftId);

        boolean activeReg = isActiveRegistration(shiftId, userId);
        boolean approvedOt = hasApprovedOt(userId, shiftId);
        if (!activeReg && !approvedOt) {
            throw new IllegalArgumentException(
                    "Bạn chưa đăng ký ca này (hoặc chưa có OT được duyệt).");
        }
        if (attendanceRepository.findByUserIdAndShiftId(userId, shiftId).isPresent()) {
            throw new IllegalArgumentException("Bạn đã check-in ca này rồi.");
        }

        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        if (AttendanceCalc.isTooEarlyToCheckIn(now, shift.getStartTime())) {
            throw new IllegalArgumentException(
                    "Chưa đến giờ check-in. Chỉ được check-in từ "
                            + shift.getStartTime()
                                    .minusMinutes(AttendanceCalc.CHECK_IN_TOLERANCE_MIN)
                                    .toLocalTime()
                            + ".");
        }
        if (now.isAfter(shift.getEndTime())) {
            throw new IllegalArgumentException("Ca đã kết thúc, không thể check-in.");
        }

        AttendanceCalc.CheckInResult ci = AttendanceCalc.resolveCheckIn(now, shift.getStartTime());

        TAttendanceRecordEntity record =
                TAttendanceRecordEntity.builder()
                        .userId(userId)
                        .shiftId(shiftId)
                        .checkInTime(now)
                        .attendStatus(ci.status())
                        .lateMinutes(ci.lateMinutes())
                        .isOvertime(approvedOt && !activeReg)
                        .build();
        TAttendanceRecordEntity saved = attendanceRepository.save(record);

        TUserEntity user = userRepository.findById(userId).orElse(null);
        notifyTelegramSafe(
                () -> {
                    String name = user != null ? user.getFullName() : "ID " + userId;
                    String statusLabel =
                            ci.status() == AttendanceStatusEnum.LATE
                                    ? ("⚠️ Trễ " + ci.lateMinutes() + " phút")
                                    : "✅ Đúng giờ";
                    telegramService.sendAttendanceNotification(
                            String.format(
                                    "🟢 Check-in: %s\nCa %d (%s–%s) · %s",
                                    name,
                                    shift.getShiftNumber(),
                                    shift.getStartTime().toLocalTime(),
                                    shift.getEndTime().toLocalTime(),
                                    statusLabel));
                });
        return toAttendanceResponse(saved, user, shift);
    }

    // ================================================================== check-out

    @Override
    @Transactional
    public AttendanceRecordResponse checkOut(Long userId, Long shiftId, String reason) {
        TWorkShiftEntity shift = findShiftOrThrow(shiftId);
        TAttendanceRecordEntity record =
                attendanceRepository
                        .findByUserIdAndShiftId(userId, shiftId)
                        .orElseThrow(() -> new ResourceNotFoundException("Chưa check-in ca này."));
        if (record.getCheckOutTime() != null) {
            throw new IllegalArgumentException("Bạn đã check-out ca này rồi.");
        }

        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        boolean needsReason = AttendanceCalc.checkoutNeedsReason(now, shift.getEndTime());
        if (needsReason && (reason == null || reason.isBlank())) {
            boolean early = now.isBefore(shift.getEndTime());
            throw new IllegalArgumentException(
                    early
                            ? "Về sớm hơn 15 phút phải kèm lý do (vd: lý do cá nhân, nhà có việc...)."
                            : "Về trễ hơn 15 phút phải kèm lý do (vd: làm thay, ca sau tới trễ...).");
        }

        record.setCheckOutTime(now);
        record.setCheckoutReason(needsReason ? reason : null);
        record.setEarlyLeaveMinutes(AttendanceCalc.earlyLeaveMinutes(now, shift.getEndTime()));
        record.setAttendStatus(
                AttendanceCalc.resolveCheckoutStatus(
                        record.getAttendStatus(), now, shift.getEndTime()));

        // Giờ công sơ bộ = giờ trong ca (cap tại cuối ca). Bù handover sẽ tính lại ở job đối soát.
        long inShift =
                AttendanceCalc.workedMinutesInShift(
                        record.getCheckInTime(), now, shift.getStartTime(), shift.getEndTime());
        record.setHoursWorked(AttendanceCalc.roundShiftHours(inShift));

        TAttendanceRecordEntity saved = attendanceRepository.save(record);
        TUserEntity user = userRepository.findById(userId).orElse(null);
        notifyTelegramSafe(
                () -> {
                    String name = user != null ? user.getFullName() : "ID " + userId;
                    telegramService.sendAttendanceNotification(
                            String.format(
                                    "🔴 Check-out: %s\nCa %d · %.1f giờ%s",
                                    name,
                                    shift.getShiftNumber(),
                                    saved.getHoursWorked().doubleValue(),
                                    needsReason ? (" · Lý do: " + reason) : ""));
                });
        return toAttendanceResponse(saved, user, shift);
    }

    // ==================================================================== queries

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
        return mapWithUserAndShift(records);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getCurrentOnShift() {
        return mapWithUserAndShift(
                attendanceRepository.findByCheckInTimeIsNotNullAndCheckOutTimeIsNull());
    }

    // ================================================================= reconcile

    @Override
    @Scheduled(fixedRate = 15 * 60 * 1000L)
    @Transactional
    public int reconcileEndedShifts() {
        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        LocalDateTime upper = now.minusMinutes(RECONCILE_GRACE_MINUTES);
        LocalDateTime lower = now.minusHours(RECONCILE_LOOKBACK_HOURS);

        List<TWorkShiftEntity> endedShifts = workShiftRepository.findByEndTimeBetween(lower, upper);
        int processed = 0;
        Set<LocalDate> affectedDates = new java.util.HashSet<>();

        for (TWorkShiftEntity shift : endedShifts) {
            affectedDates.add(shift.getShiftDate());
            List<TShiftRegistrationEntity> regs =
                    registrationRepository.findByShiftId(shift.getId()).stream()
                            .filter(
                                    r ->
                                            r.getStatus() == ShiftRegistrationStatusEnum.REGISTERED
                                                    || r.getStatus()
                                                            == ShiftRegistrationStatusEnum
                                                                    .ADMIN_ASSIGNED)
                            .toList();
            Map<Long, TAttendanceRecordEntity> recByUser =
                    attendanceRepository.findByShiftId(shift.getId()).stream()
                            .collect(Collectors.toMap(TAttendanceRecordEntity::getUserId, r -> r));

            for (TShiftRegistrationEntity reg : regs) {
                TAttendanceRecordEntity rec = recByUser.get(reg.getUserId());
                if (rec == null) {
                    if (hasApprovedLeave(reg.getUserId(), shift.getShiftDate())) {
                        continue; // nghỉ phép đã duyệt → không tính vắng
                    }
                    attendanceRepository.save(
                            TAttendanceRecordEntity.builder()
                                    .userId(reg.getUserId())
                                    .shiftId(shift.getId())
                                    .attendStatus(AttendanceStatusEnum.ABSENT)
                                    .hoursWorked(BigDecimal.ZERO)
                                    .isOvertime(false)
                                    .note("Tự động: vắng (đăng ký ca nhưng không check-in)")
                                    .build());
                    reg.setStatus(ShiftRegistrationStatusEnum.ABSENT);
                    registrationRepository.save(reg);
                    processed++;
                } else if (rec.getCheckInTime() != null && rec.getCheckOutTime() == null) {
                    // Quên check-out → chốt tại cuối ca, đánh dấu AUTO_CHECKOUT, báo Telegram.
                    rec.setCheckOutTime(shift.getEndTime());
                    rec.setAttendStatus(AttendanceStatusEnum.AUTO_CHECKOUT);
                    long inShift =
                            AttendanceCalc.workedMinutesInShift(
                                    rec.getCheckInTime(),
                                    shift.getEndTime(),
                                    shift.getStartTime(),
                                    shift.getEndTime());
                    rec.setHoursWorked(AttendanceCalc.roundShiftHours(inShift));
                    String prefix = rec.getNote() != null ? rec.getNote() + " · " : "";
                    rec.setNote(prefix + "Tự động check-out lúc kết thúc ca (quên check-out)");
                    attendanceRepository.save(rec);
                    reg.setStatus(ShiftRegistrationStatusEnum.COMPLETED);
                    registrationRepository.save(reg);

                    final Long uid = reg.getUserId();
                    notifyTelegramSafe(
                            () -> {
                                TUserEntity u = userRepository.findById(uid).orElse(null);
                                telegramService.sendAttendanceNotification(
                                        String.format(
                                                "⏰ Quên check-out: %s (Ca %d) → hệ thống tự chốt"
                                                        + " lúc %s",
                                                u != null ? u.getFullName() : "ID " + uid,
                                                shift.getShiftNumber(),
                                                shift.getEndTime().toLocalTime()));
                            });
                    processed++;
                } else if (rec.getCheckOutTime() != null
                        && reg.getStatus() != ShiftRegistrationStatusEnum.COMPLETED) {
                    reg.setStatus(ShiftRegistrationStatusEnum.COMPLETED);
                    registrationRepository.save(reg);
                }
            }
        }

        // Tính lại giờ công có bù handover cho từng NGÀY đã kết thúc hoàn toàn.
        for (LocalDate date : affectedDates) {
            recomputeRedistributionForDate(date, now);
        }

        if (processed > 0) {
            log.info("Đối soát ca: xử lý {} bản ghi (auto check-out + ABSENT)", processed);
        }
        return processed;
    }

    /** Bù handover giữa ca cho 1 ngày (chỉ khi ngày đã kết thúc hẳn). Idempotent. */
    private void recomputeRedistributionForDate(LocalDate date, LocalDateTime now) {
        List<TWorkShiftEntity> dayShifts = workShiftRepository.findByShiftDate(date);
        if (dayShifts.isEmpty()) {
            return;
        }
        LocalDateTime lastEnd =
                dayShifts.stream()
                        .map(TWorkShiftEntity::getEndTime)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
        if (lastEnd == null || now.isBefore(lastEnd.plusMinutes(RECONCILE_GRACE_MINUTES))) {
            return; // ngày chưa kết thúc hẳn — chờ lần đối soát sau
        }

        Map<Long, TWorkShiftEntity> shiftMap =
                dayShifts.stream().collect(Collectors.toMap(TWorkShiftEntity::getId, s -> s));
        List<TAttendanceRecordEntity> records = new ArrayList<>();
        for (TWorkShiftEntity s : dayShifts) {
            records.addAll(attendanceRepository.findByShiftId(s.getId()));
        }

        List<HandoverRedistribution.ShiftWindow> windows =
                dayShifts.stream()
                        .map(
                                s ->
                                        new HandoverRedistribution.ShiftWindow(
                                                s.getId(),
                                                s.getShiftNumber(),
                                                s.getStartTime(),
                                                s.getEndTime()))
                        .toList();
        List<HandoverRedistribution.AttendanceInput> inputs =
                records.stream()
                        .filter(r -> r.getCheckInTime() != null)
                        .map(
                                r ->
                                        new HandoverRedistribution.AttendanceInput(
                                                r.getId(),
                                                r.getShiftId(),
                                                r.getCheckInTime(),
                                                r.getCheckOutTime(),
                                                Boolean.TRUE.equals(r.getIsOvertime())))
                        .toList();

        Map<Long, Integer> deltas = HandoverRedistribution.compute(windows, inputs);

        for (TAttendanceRecordEntity r : records) {
            if (r.getCheckInTime() == null || r.getCheckOutTime() == null) {
                continue;
            }
            TWorkShiftEntity s = shiftMap.get(r.getShiftId());
            if (s == null) {
                continue;
            }
            int delta = deltas.getOrDefault(r.getId(), 0);
            long inShift =
                    AttendanceCalc.workedMinutesInShift(
                            r.getCheckInTime(), r.getCheckOutTime(),
                            s.getStartTime(), s.getEndTime());
            long total = Math.max(0, inShift + delta);
            BigDecimal newHours = AttendanceCalc.roundShiftHours(total);
            if (r.getRedistributedMinutes() == null
                    || r.getRedistributedMinutes() != delta
                    || newHours.compareTo(
                                    r.getHoursWorked() != null
                                            ? r.getHoursWorked()
                                            : BigDecimal.ZERO)
                            != 0) {
                r.setRedistributedMinutes(delta);
                r.setHoursWorked(newHours);
                attendanceRepository.save(r);
            }
        }
    }

    private boolean hasApprovedLeave(Long userId, LocalDate date) {
        return !leaveRequestRepository
                .findByUserIdAndLeaveDateAndStatus(userId, date, ApprovalStatusEnum.APPROVED)
                .isEmpty();
    }

    // ===================================================================== helpers

    private TWorkShiftEntity findShiftOrThrow(Long shiftId) {
        return workShiftRepository
                .findById(shiftId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Ca làm việc không tồn tại: " + shiftId));
    }

    private void notifyTelegramSafe(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            log.warn("Telegram notify failed: {}", e.getMessage());
        }
    }

    private List<AttendanceRecordResponse> mapWithUserAndShift(
            List<TAttendanceRecordEntity> records) {
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
                .lateMinutes(record.getLateMinutes())
                .earlyLeaveMinutes(record.getEarlyLeaveMinutes())
                .redistributedMinutes(record.getRedistributedMinutes())
                .checkoutReason(record.getCheckoutReason())
                .isOvertime(record.getIsOvertime())
                .note(record.getNote())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
