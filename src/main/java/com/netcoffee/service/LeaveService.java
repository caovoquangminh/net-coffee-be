package com.netcoffee.service;

import com.netcoffee.dto.response.LeaveRequestResponse;
import com.netcoffee.enumtype.LeaveTypeEnum;
import java.time.LocalDate;
import java.util.List;

public interface LeaveService {

    /** NV tạo đơn nghỉ → gửi Telegram duyệt. */
    LeaveRequestResponse create(
            Long userId, Long shiftId, LocalDate date, LeaveTypeEnum type, String reason);

    /** Duyệt đơn nghỉ (reconcile sẽ không tính ABSENT cho ngày đã duyệt nghỉ). */
    LeaveRequestResponse approve(Long id);

    /** Từ chối đơn nghỉ. */
    LeaveRequestResponse reject(Long id);

    /** Danh sách đơn nghỉ (admin: tất cả; staff: của mình). */
    List<LeaveRequestResponse> list(Long userId, boolean isAdmin);
}
