package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.response.PayrollPeriodResponse;
import com.netcoffee.dto.response.PayrollRecordResponse;
import com.netcoffee.entity.TAttendanceRecordEntity;
import com.netcoffee.entity.TPayrollPeriodEntity;
import com.netcoffee.entity.TPayrollRecordEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.PayrollPeriodStatusEnum;
import com.netcoffee.enumtype.PayrollRecordStatusEnum;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.AttendanceRecordRepository;
import com.netcoffee.repository.PayrollPeriodRepository;
import com.netcoffee.repository.PayrollRecordRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.PayrollService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollPeriodRepository periodRepository;
    private final PayrollRecordRepository recordRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PayrollPeriodResponse getOrCreatePeriod(int year, int month) {
        TPayrollPeriodEntity period =
                periodRepository
                        .findByYearAndMonth(year, month)
                        .orElseGet(
                                () -> {
                                    TPayrollPeriodEntity newPeriod =
                                            TPayrollPeriodEntity.builder()
                                                    .year(year)
                                                    .month(month)
                                                    .status(PayrollPeriodStatusEnum.DRAFT)
                                                    .build();
                                    return periodRepository.save(newPeriod);
                                });
        return toPeriodResponse(period);
    }

    @Override
    @Transactional
    public List<PayrollRecordResponse> calculatePayroll(Long periodId) {
        TPayrollPeriodEntity period = findPeriodOrThrow(periodId);

        if (period.getStatus() == PayrollPeriodStatusEnum.SENT) {
            throw new IllegalArgumentException("Kỳ lương đã được gửi, không thể tính lại.");
        }

        List<TUserEntity> staffList =
                userRepository.findByRoleAndDeletedAtIsNull(UserRoleEnum.STAFF);

        LocalDate periodStart = LocalDate.of(period.getYear(), period.getMonth(), 1);
        LocalDateTime from = periodStart.atStartOfDay();
        LocalDateTime to = periodStart.plusMonths(1).atStartOfDay();

        List<PayrollRecordResponse> results = new ArrayList<>();
        for (TUserEntity staff : staffList) {
            BigDecimal totalHours =
                    attendanceRepository.sumHoursWorkedByUserIdAndDateRange(
                            staff.getId(), from, to);

            BigDecimal overtimeHours =
                    attendanceRepository.findHistoryByUserId(staff.getId(), from, to).stream()
                            .filter(
                                    a ->
                                            Boolean.TRUE.equals(a.getIsOvertime())
                                                    && a.getHoursWorked() != null)
                            .map(TAttendanceRecordEntity::getHoursWorked)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal wage =
                    staff.getHourlyWage() != null ? staff.getHourlyWage() : BigDecimal.ZERO;
            BigDecimal baseSalary =
                    totalHours.subtract(overtimeHours).max(BigDecimal.ZERO).multiply(wage);
            BigDecimal overtimePay = overtimeHours.multiply(wage).multiply(new BigDecimal("1.5"));

            Optional<TPayrollRecordEntity> existingOpt =
                    recordRepository.findByUserIdAndPeriodId(staff.getId(), periodId);

            BigDecimal bonus =
                    existingOpt.map(TPayrollRecordEntity::getBonus).orElse(BigDecimal.ZERO);
            BigDecimal penalty =
                    existingOpt.map(TPayrollRecordEntity::getPenalty).orElse(BigDecimal.ZERO);
            BigDecimal responsibility =
                    existingOpt
                            .map(TPayrollRecordEntity::getResponsibility)
                            .orElse(BigDecimal.ZERO);
            BigDecimal advance =
                    existingOpt.map(TPayrollRecordEntity::getAdvance).orElse(BigDecimal.ZERO);

            BigDecimal totalSalary =
                    baseSalary
                            .add(overtimePay)
                            .add(bonus)
                            .add(responsibility)
                            .subtract(penalty)
                            .subtract(advance);

            TPayrollRecordEntity record =
                    existingOpt
                            .map(
                                    r -> {
                                        r.setTotalHours(totalHours);
                                        r.setHourlyWage(wage);
                                        r.setBaseSalary(baseSalary);
                                        r.setOvertimeHours(overtimeHours);
                                        r.setOvertimePay(overtimePay);
                                        r.setTotalSalary(totalSalary);
                                        return r;
                                    })
                            .orElseGet(
                                    () ->
                                            TPayrollRecordEntity.builder()
                                                    .periodId(periodId)
                                                    .userId(staff.getId())
                                                    .totalHours(totalHours)
                                                    .hourlyWage(wage)
                                                    .baseSalary(baseSalary)
                                                    .overtimeHours(overtimeHours)
                                                    .overtimePay(overtimePay)
                                                    .bonus(bonus)
                                                    .penalty(penalty)
                                                    .responsibility(responsibility)
                                                    .advance(advance)
                                                    .totalSalary(totalSalary)
                                                    .payStatus(PayrollRecordStatusEnum.PENDING)
                                                    .build());

            recordRepository.save(record);
            results.add(toRecordResponse(record, staff, period));
        }
        return results;
    }

    @Override
    @Transactional
    public PayrollRecordResponse updateManualFields(
            Long recordId,
            BigDecimal bonus,
            BigDecimal penalty,
            BigDecimal responsibility,
            BigDecimal advance) {
        TPayrollRecordEntity record = findRecordOrThrow(recordId);

        TPayrollPeriodEntity period = findPeriodOrThrow(record.getPeriodId());
        if (period.getStatus() == PayrollPeriodStatusEnum.SENT) {
            throw new IllegalArgumentException("Kỳ lương đã được gửi, không thể chỉnh sửa.");
        }

        if (bonus != null) record.setBonus(bonus);
        if (penalty != null) record.setPenalty(penalty);
        if (responsibility != null) record.setResponsibility(responsibility);
        if (advance != null) record.setAdvance(advance);

        // Recalculate total — clamp to 0 (deductions cannot exceed earnings)
        BigDecimal totalSalary =
                record.getBaseSalary()
                        .add(record.getOvertimePay())
                        .add(record.getBonus())
                        .add(record.getResponsibility())
                        .subtract(record.getPenalty())
                        .subtract(record.getAdvance())
                        .max(BigDecimal.ZERO);
        record.setTotalSalary(totalSalary);

        recordRepository.save(record);

        TUserEntity user = userRepository.findById(record.getUserId()).orElse(null);
        return toRecordResponse(record, user, period);
    }

    @Override
    @Transactional
    public PayrollPeriodResponse sendPayroll(Long periodId) {
        TPayrollPeriodEntity period = findPeriodOrThrow(periodId);
        period.setStatus(PayrollPeriodStatusEnum.SENT);
        period.setSentAt(LocalDateTime.now(AppConstant.VN_ZONE));
        return toPeriodResponse(periodRepository.save(period));
    }

    @Override
    @Transactional
    public PayrollRecordResponse confirmPayroll(Long recordId, Long userId) {
        TPayrollRecordEntity record = findRecordOrThrow(recordId);
        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xác nhận bảng lương này");
        }
        record.setPayStatus(PayrollRecordStatusEnum.CONFIRMED);
        record.setDisputeReason(null);
        recordRepository.save(record);

        TUserEntity user = userRepository.findById(userId).orElse(null);
        TPayrollPeriodEntity period = findPeriodOrThrow(record.getPeriodId());
        return toRecordResponse(record, user, period);
    }

    @Override
    @Transactional
    public PayrollRecordResponse disputePayroll(Long recordId, Long userId, String reason) {
        TPayrollRecordEntity record = findRecordOrThrow(recordId);
        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền phản hồi bảng lương này");
        }
        record.setPayStatus(PayrollRecordStatusEnum.DISPUTED);
        record.setDisputeReason(reason);
        recordRepository.save(record);

        TUserEntity user = userRepository.findById(userId).orElse(null);
        TPayrollPeriodEntity period = findPeriodOrThrow(record.getPeriodId());
        return toRecordResponse(record, user, period);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollRecordResponse> getMyPayroll(Long userId) {
        List<TPayrollRecordEntity> records = recordRepository.findByUserId(userId);
        TUserEntity user = userRepository.findById(userId).orElse(null);

        Set<Long> periodIds =
                records.stream().map(TPayrollRecordEntity::getPeriodId).collect(Collectors.toSet());
        Map<Long, TPayrollPeriodEntity> periodMap =
                periodRepository.findAllById(periodIds).stream()
                        .collect(Collectors.toMap(TPayrollPeriodEntity::getId, p -> p));

        // Only return records where period is SENT or CONFIRMED
        return records.stream()
                .filter(
                        r -> {
                            TPayrollPeriodEntity p = periodMap.get(r.getPeriodId());
                            return p != null && p.getStatus() != PayrollPeriodStatusEnum.DRAFT;
                        })
                .map(r -> toRecordResponse(r, user, periodMap.get(r.getPeriodId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollRecordResponse> getAllPayroll(Long periodId) {
        TPayrollPeriodEntity period = findPeriodOrThrow(periodId);
        List<TPayrollRecordEntity> records = recordRepository.findByPeriodId(periodId);

        Set<Long> userIds =
                records.stream().map(TPayrollRecordEntity::getUserId).collect(Collectors.toSet());
        Map<Long, TUserEntity> userMap =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(TUserEntity::getId, u -> u));

        return records.stream()
                .map(r -> toRecordResponse(r, userMap.get(r.getUserId()), period))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollPeriodResponse> getAllPeriods() {
        return periodRepository.findAll().stream().map(this::toPeriodResponse).toList();
    }

    private TPayrollPeriodEntity findPeriodOrThrow(Long periodId) {
        return periodRepository
                .findById(periodId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Kỳ lương không tồn tại: " + periodId));
    }

    private TPayrollRecordEntity findRecordOrThrow(Long recordId) {
        return recordRepository
                .findById(recordId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Bản ghi lương không tồn tại: " + recordId));
    }

    private PayrollPeriodResponse toPeriodResponse(TPayrollPeriodEntity p) {
        return PayrollPeriodResponse.builder()
                .id(p.getId())
                .year(p.getYear())
                .month(p.getMonth())
                .status(p.getStatus())
                .sentAt(p.getSentAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PayrollRecordResponse toRecordResponse(
            TPayrollRecordEntity r, TUserEntity user, TPayrollPeriodEntity period) {
        return PayrollRecordResponse.builder()
                .id(r.getId())
                .periodId(r.getPeriodId())
                .year(period != null ? period.getYear() : null)
                .month(period != null ? period.getMonth() : null)
                .userId(r.getUserId())
                .userName(user != null ? user.getFullName() : null)
                .totalHours(r.getTotalHours())
                .hourlyWage(r.getHourlyWage())
                .baseSalary(r.getBaseSalary())
                .overtimeHours(r.getOvertimeHours())
                .overtimePay(r.getOvertimePay())
                .bonus(r.getBonus())
                .penalty(r.getPenalty())
                .responsibility(r.getResponsibility())
                .advance(r.getAdvance())
                .totalSalary(r.getTotalSalary())
                .payStatus(r.getPayStatus())
                .disputeReason(r.getDisputeReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
