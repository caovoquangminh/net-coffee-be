package com.netcoffee.service;

import com.netcoffee.dto.response.AttendanceRecordResponse;
import com.netcoffee.dto.response.ShiftResponse;
import java.time.LocalDate;
import java.util.List;

public interface ShiftService {

    /** Tạo 3 ca làm việc cho ngày chỉ định (idempotent). */
    void generateShiftsForDate(LocalDate date);

    /** Lịch ca làm việc trong khoảng ngày, kèm danh sách đăng ký. */
    List<ShiftResponse> getShiftsForDateRange(LocalDate from, LocalDate to);

    /** Đăng ký ca làm việc (tối đa 2 người đã APPROVED). */
    ShiftResponse registerShift(Long userId, Long shiftId);

    /** Admin phê duyệt đăng ký ca. */
    ShiftResponse approveRegistration(Long registrationId);

    /** Hủy đăng ký ca. */
    void cancelRegistration(Long registrationId, Long userId, boolean isAdmin);

    /** Chấm công vào. */
    AttendanceRecordResponse checkIn(Long userId, Long shiftId);

    /** Chấm công ra, tính giờ làm. */
    AttendanceRecordResponse checkOut(Long userId, Long shiftId);

    /** Lịch sử chấm công (admin: mọi user; staff: chỉ của mình). */
    List<AttendanceRecordResponse> getAttendanceHistory(Long userId, LocalDate from, LocalDate to);

    /** Danh sách nhân viên đang ca (đã check-in, chưa check-out). */
    List<AttendanceRecordResponse> getCurrentOnShift();

    /**
     * Đối soát các ca đã kết thúc: tự động check-out cho người quên check-out (chốt giờ tại cuối
     * ca) và đánh dấu ABSENT cho người được duyệt ca nhưng không đến. Chạy định kỳ để bịt kẽ hở
     * "quên check-out → giờ vô hạn" và "vắng không bị ghi nhận".
     *
     * @return số bản ghi đã xử lý (auto check-out + absent)
     */
    int reconcileEndedShifts();
}
