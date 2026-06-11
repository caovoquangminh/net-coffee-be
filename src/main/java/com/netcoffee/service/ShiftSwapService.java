package com.netcoffee.service;

import com.netcoffee.dto.response.ShiftSwapResponse;
import java.util.List;

public interface ShiftSwapService {

    /** NV tạo yêu cầu đổi ca (nhường ca cho người khác) → gửi Telegram duyệt. */
    ShiftSwapResponse create(Long fromUserId, Long toUserId, Long shiftId, String reason);

    /** Duyệt: chuyển đăng ký ca từ người nhường sang người nhận. */
    ShiftSwapResponse approve(Long id);

    /** Từ chối: giữ nguyên ca của người nhường. */
    ShiftSwapResponse reject(Long id);

    /** Danh sách yêu cầu đổi ca (admin: tất cả; staff: liên quan tới mình). */
    List<ShiftSwapResponse> list(Long userId, boolean isAdmin);
}
