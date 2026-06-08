package com.netcoffee.service;

import com.netcoffee.entity.TOvertimeRequestEntity;

public interface TelegramService {

    /**
     * Gửi thông báo yêu cầu OT cho admin với inline keyboard [Approve / Reject].
     *
     * @return message_id của message đã gửi, hoặc null nếu không gửi được (bot chưa cấu hình)
     */
    String sendOvertimeRequest(
            TOvertimeRequestEntity request, String requesterName, String shiftInfo);

    /** Gửi thông báo văn bản đơn giản cho admin chat. No-op nếu bot-token chưa cấu hình. */
    void sendAttendanceNotification(String message);
}
