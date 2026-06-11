package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.service.LeaveService;
import com.netcoffee.service.OvertimeService;
import com.netcoffee.service.ShiftSwapService;
import com.netcoffee.service.TelegramService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ApiPaths.WEBHOOK_TELEGRAM)
@RequiredArgsConstructor
public class TelegramWebhookController {

    @Value("${app.telegram.webhook-secret:changeme}")
    private String webhookSecret;

    private final OvertimeService overtimeService;
    private final ShiftSwapService shiftSwapService;
    private final LeaveService leaveService;
    private final TelegramService telegramService;

    @PostMapping
    public ResponseEntity<Void> handleTelegramWebhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false)
                    String secretToken,
            @RequestBody Map<String, Object> body) {

        if (webhookSecret != null
                && !webhookSecret.isBlank()
                && !webhookSecret.equals(secretToken)) {
            log.warn("Telegram webhook: invalid secret token");
            return ResponseEntity.status(403).build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> callbackQuery = (Map<String, Object>) body.get("callback_query");
        if (callbackQuery == null) {
            return ResponseEntity.ok().build();
        }
        String data = (String) callbackQuery.get("data");
        String callbackId = (String) callbackQuery.get("id");
        if (data == null) {
            return ResponseEntity.ok().build();
        }
        log.info("Telegram callback_query data: {}", data);

        String resultText = "Đã xử lý";
        try {
            resultText = dispatch(data);
        } catch (Exception e) {
            log.error("Lỗi xử lý callback Telegram: {}", e.getMessage());
            resultText = "Lỗi: " + e.getMessage();
        }
        try {
            telegramService.answerCallback(callbackId, resultText);
        } catch (Exception ignored) {
            // best-effort
        }
        return ResponseEntity.ok().build();
    }

    /** Định tuyến callback_data dạng {@code <category>_<action>:<id>}. */
    private String dispatch(String data) {
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
            default -> {
                return "Hành động không xác định: " + prefix;
            }
        }
    }
}
