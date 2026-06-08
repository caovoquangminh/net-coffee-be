package com.netcoffee.repository;

import com.netcoffee.entity.TAttendanceRecordEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<TAttendanceRecordEntity, Long> {

    List<TAttendanceRecordEntity> findByUserId(Long userId);

    List<TAttendanceRecordEntity> findByShiftId(Long shiftId);

    Optional<TAttendanceRecordEntity> findByUserIdAndShiftId(Long userId, Long shiftId);

    /** Tổng giờ làm của một nhân viên trong khoảng thời gian (check_in_time giữa from và to). */
    @Query(
            "SELECT COALESCE(SUM(a.hoursWorked), 0) FROM TAttendanceRecordEntity a "
                    + "WHERE a.userId = :userId AND a.checkInTime >= :from AND a.checkInTime < :to")
    BigDecimal sumHoursWorkedByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Danh sách nhân viên đang làm việc (đã check-in nhưng chưa check-out). */
    List<TAttendanceRecordEntity> findByCheckInTimeIsNotNullAndCheckOutTimeIsNull();

    /** Đếm số bản ghi đang on-shift (đã check-in, chưa check-out) — dùng cho dashboard. */
    long countByCheckInTimeIsNotNullAndCheckOutTimeIsNull();

    /** Tổng (hours_worked * hourly_wage) ước tính cho tháng hiện tại — join với users. */
    @Query(
            nativeQuery = true,
            value =
                    "SELECT COALESCE(SUM(a.hours_worked * u.hourly_wage), 0) "
                            + "FROM attendance_records a "
                            + "JOIN users u ON u.id = a.user_id "
                            + "WHERE a.check_in_time >= :from AND a.check_in_time < :to "
                            + "  AND u.hourly_wage IS NOT NULL")
    BigDecimal sumWageEstimateBetween(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Lịch sử chấm công của một nhân viên cụ thể trong khoảng ngày. */
    @Query(
            "SELECT a FROM TAttendanceRecordEntity a "
                    + "WHERE a.userId = :userId "
                    + "AND a.checkInTime >= :from AND a.checkInTime < :to "
                    + "ORDER BY a.checkInTime ASC")
    List<TAttendanceRecordEntity> findHistoryByUserId(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Lịch sử chấm công tất cả nhân viên trong khoảng ngày. */
    @Query(
            "SELECT a FROM TAttendanceRecordEntity a "
                    + "WHERE a.checkInTime >= :from AND a.checkInTime < :to "
                    + "ORDER BY a.checkInTime ASC")
    List<TAttendanceRecordEntity> findHistoryAll(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
