package com.netcoffee.service;

import com.netcoffee.dto.response.OvertimeRequestResponse;
import com.netcoffee.enumtype.OvertimeTypeEnum;
import java.util.List;

public interface OvertimeService {

    /** Tạo yêu cầu OT, gửi Telegram cho admin. */
    OvertimeRequestResponse createOvertimeRequest(
            Long requesterId,
            Long shiftId,
            String reason,
            OvertimeTypeEnum type,
            Long coveringUserId);

    /** Admin phê duyệt OT, tạo attendance record cho OT. */
    OvertimeRequestResponse approveOvertime(Long requestId, Long approvedBy);

    /** Admin từ chối OT. */
    OvertimeRequestResponse rejectOvertime(Long requestId);

    /** Lấy danh sách yêu cầu OT (admin: tất cả; staff: của mình). */
    List<OvertimeRequestResponse> getOvertimeRequests(Long userId, boolean isAdmin);
}
