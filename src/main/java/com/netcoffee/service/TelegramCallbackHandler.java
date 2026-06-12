package com.netcoffee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Xử lý callback_data nút bấm Telegram dạng {@code <category>_<action>:<id>}. Dùng chung cho cả
 * webhook (production) và long-polling (local).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramCallbackHandler {

    private final OvertimeService overtimeService;
    private final ShiftSwapService shiftSwapService;
    private final LeaveService leaveService;
    private final ShiftTransferService shiftTransferService;

    /**
     * @return text phản hồi hiển thị cho admin.
     */
    public String handle(String data) {
        if (data == null) {
            return "Dữ liệu rỗng";
        }
        int colon = data.indexOf(':');
        if (colon < 0) {
            return "Dữ liệu không hợp lệ";
        }
        String prefix = data.substring(0, colon);
        Long id = Long.parseLong(data.substring(colon + 1));
        switch (prefix) {
            case "ot_approve" -> {
                overtimeService.approveOvertime(id, null);
                return "✅ Đã duyệt OT #" + id;
            }
            case "ot_reject" -> {
                overtimeService.rejectOvertime(id);
                return "❌ Đã từ chối OT #" + id;
            }
            case "swap_approve" -> {
                shiftSwapService.approve(id);
                return "✅ Đã duyệt đổi ca #" + id;
            }
            case "swap_reject" -> {
                shiftSwapService.reject(id);
                return "❌ Đã từ chối đổi ca #" + id;
            }
            case "leave_approve" -> {
                leaveService.approve(id);
                return "✅ Đã duyệt nghỉ #" + id;
            }
            case "leave_reject" -> {
                leaveService.reject(id);
                return "❌ Đã từ chối nghỉ #" + id;
            }
            case "transfer_approve" -> {
                shiftTransferService.approve(id, null);
                return "✅ Đã duyệt làm thay #" + id;
            }
            case "transfer_reject" -> {
                shiftTransferService.reject(id);
                return "❌ Đã từ chối làm thay #" + id;
            }
            default -> {
                return "Hành động không xác định: " + prefix;
            }
        }
    }
}
