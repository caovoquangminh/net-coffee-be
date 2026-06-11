package com.netcoffee.service;

import com.netcoffee.entity.TOvertimeRequestEntity;

public interface TelegramService {

    /**
     * Gửi thông báo yêu cầu OT cho admin với inline keyboard [Duyệt / Từ chối].
     *
     * @return message_id của message đã gửi, hoặc null nếu không gửi được (bot chưa cấu hình)
     */
    String sendOvertimeRequest(
            TOvertimeRequestEntity request, String requesterName, String shiftInfo);

    /**
     * Gửi yêu cầu cần duyệt (đổi ca / nghỉ phép / OT...) kèm inline keyboard. Callback data dạng
     * {@code <category>_approve:<id>} và {@code <category>_reject:<id>}.
     *
     * @param category nhóm yêu cầu: "ot", "swap", "leave"
     * @return message_id hoặc null
     */
    String sendApprovalRequest(String category, Long id, String text);

    /** Gửi thông báo văn bản đơn giản cho admin chat. No-op nếu bot chưa cấu hình. */
    void sendAttendanceNotification(String message);

    /** Phản hồi nút bấm (tắt vòng loading trên Telegram). Best-effort. */
    void answerCallback(String callbackQueryId, String text);
}
