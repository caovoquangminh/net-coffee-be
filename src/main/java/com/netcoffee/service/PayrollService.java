package com.netcoffee.service;

import com.netcoffee.dto.response.PayrollPeriodResponse;
import com.netcoffee.dto.response.PayrollRecordResponse;
import java.math.BigDecimal;
import java.util.List;

public interface PayrollService {

    /** Tạo hoặc lấy kỳ lương theo tháng (idempotent). */
    PayrollPeriodResponse getOrCreatePeriod(int year, int month);

    /**
     * Tính lại toàn bộ lương cho kỳ (từ attendance_records), giữ
     * bonus/penalty/responsibility/advance.
     */
    List<PayrollRecordResponse> calculatePayroll(Long periodId);

    /** Admin cập nhật các trường thủ công (bonus, penalty, responsibility, advance). */
    PayrollRecordResponse updateManualFields(
            Long recordId,
            BigDecimal bonus,
            BigDecimal penalty,
            BigDecimal responsibility,
            BigDecimal advance);

    /** Gửi bảng lương cho nhân viên (status → SENT). */
    PayrollPeriodResponse sendPayroll(Long periodId);

    /** Nhân viên xác nhận lương. */
    PayrollRecordResponse confirmPayroll(Long recordId, Long userId);

    /** Nhân viên phản hồi lương với lý do. */
    PayrollRecordResponse disputePayroll(Long recordId, Long userId, String reason);

    /** Nhân viên xem lịch sử bảng lương của mình. */
    List<PayrollRecordResponse> getMyPayroll(Long userId);

    /** Admin xem toàn bộ bảng lương trong kỳ. */
    List<PayrollRecordResponse> getAllPayroll(Long periodId);

    /** Lấy tất cả kỳ lương. */
    List<PayrollPeriodResponse> getAllPeriods();
}
