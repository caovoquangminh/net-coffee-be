package com.netcoffee.service;

import com.netcoffee.dto.response.ShiftTransferResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface ShiftTransferService {

    /** A tạo yêu cầu nhờ B làm thay đoạn [start,end] của ca → Telegram duyệt. */
    ShiftTransferResponse create(
            Long originalUserId,
            Long shiftId,
            Long replacementUserId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String reason);

    /** Duyệt → chia ca thành đoạn cho A và B (shift_assignments). */
    ShiftTransferResponse approve(Long id, Long approvedBy);

    ShiftTransferResponse reject(Long id);

    /** Danh sách (admin: tất cả; staff: liên quan tới mình). */
    List<ShiftTransferResponse> list(Long userId, boolean isAdmin);
}
