package com.netcoffee.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcoffee.entity.TOvertimeRequestEntity;
import com.netcoffee.service.AppSettingService;
import com.netcoffee.service.TelegramService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramServiceImpl implements TelegramService {

    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    @Value("${app.telegram.bot-token:}")
    private String botTokenYml;

    @Value("${app.telegram.admin-chat-id:}")
    private String adminChatIdYml;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AppSettingService appSettingService;

    /** Token/chat id ưu tiên lấy từ cấu hình admin (app_settings), fallback application.yml. */
    private String token() {
        return appSettingService.get(AppSettingService.TELEGRAM_BOT_TOKEN, botTokenYml);
    }

    private String chatId() {
        return appSettingService.get(AppSettingService.TELEGRAM_CHAT_ID, adminChatIdYml);
    }

    private boolean isConfigured() {
        String t = token();
        String c = chatId();
        return t != null && !t.isBlank() && c != null && !c.isBlank();
    }

    @Override
    public String sendOvertimeRequest(
            TOvertimeRequestEntity request, String requesterName, String shiftInfo) {
        String otRange =
                (request.getOtStartTime() != null && request.getOtEndTime() != null)
                        ? ("\nGiờ OT: "
                                + request.getOtStartTime().toLocalTime()
                                + " - "
                                + request.getOtEndTime().toLocalTime())
                        : "";
        String text =
                String.format(
                        "📋 *Yêu cầu tăng ca mới*\n"
                                + "Nhân viên: %s\n"
                                + "Ca: %s%s\n"
                                + "Loại: %s\n"
                                + "Lý do: %s\n"
                                + "ID yêu cầu: %d",
                        requesterName,
                        shiftInfo,
                        otRange,
                        request.getOtType() != null ? request.getOtType().name() : "N/A",
                        request.getReason() != null ? request.getReason() : "Không có",
                        request.getId());
        return sendApprovalRequest("ot", request.getId(), text);
    }

    @Override
    public String sendApprovalRequest(String category, Long id, String text) {
        if (!isConfigured()) {
            log.warn("Telegram chưa cấu hình. Bỏ qua gửi yêu cầu duyệt {}:{}", category, id);
            return null;
        }
        Map<String, Object> inlineKeyboard =
                Map.of(
                        "inline_keyboard",
                        List.of(
                                List.of(
                                        Map.of(
                                                "text",
                                                "✅ Duyệt",
                                                "callback_data",
                                                category + "_approve:" + id),
                                        Map.of(
                                                "text",
                                                "❌ Từ chối",
                                                "callback_data",
                                                category + "_reject:" + id))));
        Map<String, Object> payload =
                Map.of(
                        "chat_id",
                        chatId(),
                        "text",
                        text,
                        "parse_mode",
                        "Markdown",
                        "reply_markup",
                        inlineKeyboard);
        return postMessage(payload);
    }

    @Override
    public void sendAttendanceNotification(String message) {
        if (!isConfigured()) {
            log.warn("Telegram chưa cấu hình. Bỏ qua thông báo chấm công.");
            return;
        }
        postMessage(Map.of("chat_id", chatId(), "text", message));
    }

    @Override
    public void answerCallback(String callbackQueryId, String text) {
        if (!isConfigured() || callbackQueryId == null) {
            return;
        }
        try {
            String url = TELEGRAM_API + token() + "/answerCallbackQuery";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body =
                    objectMapper.writeValueAsString(
                            Map.of("callback_query_id", callbackQueryId, "text", text));
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.warn("answerCallbackQuery thất bại: {}", e.getMessage());
        }
    }

    /** Gửi sendMessage, trả message_id nếu thành công. */
    @SuppressWarnings("unchecked")
    private String postMessage(Map<String, Object> payload) {
        try {
            String url = TELEGRAM_API + token() + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(payload);
            Map<String, Object> response =
                    restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                return result != null ? String.valueOf(result.get("message_id")) : null;
            }
        } catch (Exception e) {
            log.error("Gửi Telegram thất bại: {}", e.getMessage());
        }
        return null;
    }
}
