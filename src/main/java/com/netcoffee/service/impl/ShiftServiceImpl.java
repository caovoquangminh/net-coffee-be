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
    private final com.netcoffee.repository.ShiftAssignmentRepository shiftAssignmentRepository;

    /**
     * Cửa sổ giờ hiệu lực của NV trong ca = đoạn ca (nếu có chia do làm thay), ngược lại trọn ca.
     */
    private LocalDateTime[] effectiveWindow(Long shiftId, Long userId, TWorkShiftEntity shift) {
        return shiftAssignmentRepository
                .findByShiftIdAndUserId(shiftId, userId)
                .map(a -> new LocalDateTime[] {a.getStartTime(), a.getEndTime()})
                .orElse(new LocalDateTime[] {shift.getStartTime(), shift.getEndTime()});
    }

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
        LocalDateTime[] win = effectiveWindow(shiftId, userId, shift);
        LocalDateTime wStart = win[0];
        LocalDateTime wEnd = win[1];
        if (AttendanceCalc.isTooEarlyToCheckIn(now, wStart)) {
            throw new IllegalArgumentException(
                    "Chưa đến giờ check-in. Chỉ được check-in từ "
                            + wStart.minusMinutes(AttendanceCalc.CHECK_IN_TOLERANCE_MIN)
                                    .toLocalTime()
                            + ".");
        }
        if (now.isAfter(wEnd)) {
            throw new IllegalArgumentException("Ca đã kết thúc, không thể check-in.");
        }

        AttendanceCalc.CheckInResult ci = AttendanceCalc.resolveCheckIn(now, wStart);

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
                                    wStart.toLocalTime(),
                                    wEnd.toLocalTime(),
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
        LocalDateTime[] win = effectiveWindow(shiftId, userId, shift);
        LocalDateTime wStart = win[0];
        LocalDateTime wEnd = win[1];
        boolean needsReason = AttendanceCalc.checkoutNeedsReason(now, wEnd);
        if (needsReason && (reason == null || reason.isBlank())) {
            boolean early = now.isBefore(wEnd);
            throw new IllegalArgumentException(
                    early
                            ? "Về sớm hơn 15 phút phải kèm lý do (vd: lý do cá nhân, nhà có việc...)."
                            : "Về trễ hơn 15 phút phải kèm lý do (vd: làm thay, ca sau tới trễ...).");
        }

        record.setCheckOutTime(now);
        record.setCheckoutReason(needsReason ? reason : null);
        record.setEarlyLeaveMinutes(AttendanceCalc.earlyLeaveMinutes(now, wEnd));
        record.setAttendStatus(
                AttendanceCalc.resolveCheckoutStatus(record.getAttendStatus(), now, wEnd));

        // Giờ công sơ bộ = giờ trong ĐOẠN của NV (cap tại cuối đoạn). Bù handover tính lại ở job.
        long inShift =
                AttendanceCalc.workedMinutesInShift(record.getCheckInTime(), now, wStart, wEnd);
        record.setHoursWorked(AttendanceCalc.roundShiftHours(inShift));

        // OT: làm THÊM sau khi hết ca + có OT duyệt → tính giờ OT (giới hạn tới giờ OT đã duyệt).
        if (now.isAfter(wEnd)) {
            overtimeRequestRepository.findByRequesterIdAndShiftId(userId, shiftId).stream()
                    .filter(o -> o.getStatus() == OvertimeStatusEnum.APPROVED)
                    .findFirst()
                    .ifPresent(
                            ot -> {
                                LocalDateTime cap =
                                        ot.getOtEndTime() != null ? ot.getOtEndTime() : now;
                                LocalDateTime otEnd = now.isBefore(cap) ? now : cap;
                                long otMin = java.time.Duration.between(wEnd, otEnd).toMinutes();
                                record.setOtHours(
                                        AttendanceCalc.roundShiftHours(Math.max(0, otMin)));
                            });
        }

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

    @Override
    @Transactional(readOnly = true)
    public com.netcoffee.dto.response.AttendanceDashboardResponse getDashboardSummary() {
        LocalDateTime now = LocalDateTime.now(AppConstant.VN_ZONE);
        LocalDate today = now.toLocalDate();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEndExcl = monthStart.plusMonths(1);

        // ---- Hôm nay
        long todayWorking = attendanceRepository.countByCheckInTimeIsNotNullAndCheckOutTimeIsNull();
        List<TAttendanceRecordEntity> todayRecords =
                attendanceRepository.findHistoryAll(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long todayLate =
                todayRecords.stream()
                        .filter(a -> a.getAttendStatus() == AttendanceStatusEnum.LATE)
                        .count();
        long todayOnLeave =
                leaveRequestRepository
                        .findByLeaveDateBetweenAndStatus(today, today, ApprovalStatusEnum.APPROVED)
                        .size();

        long todayNotCheckedIn = 0;
        for (TWorkShiftEntity s : workShiftRepository.findByShiftDate(today)) {
            if (s.getStartTime().isAfter(now)) {
                continue; // ca chưa tới giờ
            }
            long active = countActiveRegistrations(s.getId());
            long checkedIn =
                    attendanceRepository.findByShiftId(s.getId()).stream()
                            .filter(a -> a.getCheckInTime() != null)
                            .count();
            todayNotCheckedIn += Math.max(0, active - checkedIn);
        }

        // ---- Tuần
        List<TAttendanceRecordEntity> weekRecords =
                attendanceRepository.findHistoryAll(
                        weekStart.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay());
        BigDecimal weekOtHours =
                weekRecords.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getIsOvertime()))
                        .map(a -> a.getHoursWorked() != null ? a.getHoursWorked() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TWorkShiftEntity> weekShifts =
                workShiftRepository.findByShiftDateBetween(weekStart, weekEnd);
        long totalSlots = (long) weekShifts.size() * MAX_PER_SHIFT;
        long filledSlots =
                weekShifts.stream().mapToLong(s -> countActiveRegistrations(s.getId())).sum();
        int fillRate = totalSlots > 0 ? (int) Math.round(100.0 * filledSlots / totalSlots) : 0;

        // ---- Tháng
        BigDecimal monthSalaryCost =
                attendanceRepository.sumWageEstimateBetween(
                        monthStart.atStartOfDay(), monthEndExcl.atStartOfDay());
        List<TAttendanceRecordEntity> monthRecords =
                attendanceRepository.findHistoryAll(
                        monthStart.atStartOfDay(), monthEndExcl.atStartOfDay());

        Map<Long, BigDecimal> hoursByUser = new java.util.HashMap<>();
        for (TAttendanceRecordEntity a : monthRecords) {
            if (a.getHoursWorked() != null) {
                hoursByUser.merge(a.getUserId(), a.getHoursWorked(), BigDecimal::add);
            }
        }
        List<Long> topUserIds =
                hoursByUser.entrySet().stream()
                        .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                        .limit(3)
                        .map(Map.Entry::getKey)
                        .toList();
        Map<Long, TUserEntity> topUsers =
                userRepository.findAllById(topUserIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));
        List<com.netcoffee.dto.response.AttendanceDashboardResponse.TopStaff> topStaff =
                topUserIds.stream()
                        .map(
                                uid ->
                                        com.netcoffee.dto.response.AttendanceDashboardResponse
                                                .TopStaff.builder()
                                                .userId(uid)
                                                .userName(
                                                        topUsers.get(uid) != null
                                                                ? topUsers.get(uid).getFullName()
                                                                : null)
                                                .hours(hoursByUser.get(uid))
                                                .build())
                        .toList();

        return com.netcoffee.dto.response.AttendanceDashboardResponse.builder()
                .todayWorking(todayWorking)
                .todayLate(todayLate)
                .todayOnLeave(todayOnLeave)
                .todayNotCheckedIn(todayNotCheckedIn)
                .weekTotalHours(sumHours(weekRecords))
                .weekOtHours(weekOtHours)
                .weekFillRatePercent(fillRate)
                .monthSalaryCost(monthSalaryCost != null ? monthSalaryCost : BigDecimal.ZERO)
                .monthTotalHours(sumHours(monthRecords))
                .monthTopStaff(topStaff)
                .build();
    }

    private BigDecimal sumHours(List<TAttendanceRecordEntity> records) {
        return records.stream()
                .map(a -> a.getHoursWorked() != null ? a.getHoursWorked() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.netcoffee.dto.response.StaffOptionResponse> getColleagues() {
        return userRepository
                .findByRoleAndDeletedAtIsNull(com.netcoffee.enumtype.UserRoleEnum.STAFF)
                .stream()
                .map(
                        u ->
                                com.netcoffee.dto.response.StaffOptionResponse.builder()
                                        .id(u.getId())
                                        .name(
                                                u.getFullName() != null
                                                        ? u.getFullName()
                                                        : u.getPhoneNumber())
                                        .build())
                .toList();
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
                    // Quên check-out → chốt tại cuối ĐOẠN, đánh dấu AUTO_CHECKOUT, báo Telegram.
                    LocalDateTime[] w = effectiveWindow(shift.getId(), reg.getUserId(), shift);
                    rec.setCheckOutTime(w[1]);
                    rec.setAttendStatus(AttendanceStatusEnum.AUTO_CHECKOUT);
                    long inShift =
                            AttendanceCalc.workedMinutesInShift(
                                    rec.getCheckInTime(), w[1], w[0], w[1]);
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
            LocalDateTime[] w = effectiveWindow(s.getId(), r.getUserId(), s);
            long inShift =
                    AttendanceCalc.workedMinutesInShift(
                            r.getCheckInTime(), r.getCheckOutTime(), w[0], w[1]);
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
