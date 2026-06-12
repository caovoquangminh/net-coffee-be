package com.netcoffee.service;

import com.netcoffee.dto.response.AttendanceDashboardResponse;
import com.netcoffee.dto.response.AttendanceRecordResponse;
import com.netcoffee.dto.response.ShiftResponse;
import java.time.LocalDate;
import java.util.List;

public interface ShiftService {

    /** Tổng hợp widget chấm công cho dashboard admin (hôm nay/tuần/tháng). */
    AttendanceDashboardResponse getDashboardSummary();

    /** Danh sách nhân viên (id + tên) để chọn người làm thay / admin sắp ca. */
    List<com.netcoffee.dto.response.StaffOptionResponse> getColleagues();

    /** Tạo 3 ca làm việc cho ngày chỉ định (idempotent). */
    void generateShiftsForDate(LocalDate date);

    /** Lịch ca làm việc trong khoảng ngày, kèm danh sách đăng ký. */
    List<ShiftResponse> getShiftsForDateRange(LocalDate from, LocalDate to);

    /** Cửa sổ đăng ký (T6/T7/CN) hiện có đang mở cho tuần kế tiếp hay không. */
    boolean isRegistrationWindowOpen();

    /**
     * Nhân viên tự đăng ký ca (không cần duyệt). Chỉ cho đăng ký ca thuộc TUẦN KẾ TIẾP và chỉ trong
     * cửa sổ T6/T7/CN. Tối đa 2 người/ca.
     */
    ShiftResponse registerShift(Long userId, Long shiftId);

    /** Admin sắp ca chậm cho NV không đăng ký kịp (bỏ qua kiểm tra cửa sổ). */
    ShiftResponse assignShift(Long shiftId, Long userId);

    /** Hủy đăng ký ca. */
    void cancelRegistration(Long registrationId, Long userId, boolean isAdmin);

    /** Chấm công vào. */
    AttendanceRecordResponse checkIn(Long userId, Long shiftId);

    /** Chấm công ra. Sớm/trễ quá dung sai bắt buộc có {@code reason}. */
    AttendanceRecordResponse checkOut(Long userId, Long shiftId, String reason);

    /** Lịch sử chấm công (admin: mọi user; staff: chỉ của mình). */
    List<AttendanceRecordResponse> getAttendanceHistory(Long userId, LocalDate from, LocalDate to);

    /** Danh sách nhân viên đang ca (đã check-in, chưa check-out). */
    List<AttendanceRecordResponse> getCurrentOnShift();

    /**
     * Đối soát các ca đã kết thúc: auto check-out cho người quên check-out (chốt tại cuối ca), đánh
     * dấu ABSENT cho người đăng ký nhưng không đến, và tính lại giờ công có bù handover giữa ca.
     *
     * @return số bản ghi đã xử lý
     */
    int reconcileEndedShifts();

    /**
     * Dọn các ca cũ hơn {@code retentionDays} ngày (giữ lại N ngày gần nhất). Dữ liệu phụ thuộc tự
     * xóa theo CASCADE.
     *
     * @return số ca đã xóa
     */
    int purgeOldShifts(int retentionDays);
}
