package com.netcoffee.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcoffee.entity.TOvertimeRequestEntity;
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
    private String botToken;

    @Value("${app.telegram.admin-chat-id:}")
    private String adminChatId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String sendOvertimeRequest(
            TOvertimeRequestEntity request, String requesterName, String shiftInfo) {
        if (!isConfigured()) {
            log.warn("Telegram bot-token not configured. Skipping OT request notification.");
            return null;
        }

        String text =
                String.format(
                        "📋 *Yêu cầu tăng ca mới*\n"
                                + "Nhân viên: %s\n"
                                + "Ca: %s\n"
                                + "Loại: %s\n"
                                + "Lý do: %s\n"
                                + "ID yêu cầu: %d",
                        requesterName,
                        shiftInfo,
                        request.getOtType() != null ? request.getOtType().name() : "N/A",
                        request.getReason() != null ? request.getReason() : "Không có",
                        request.getId());

        Map<String, Object> inlineKeyboard =
                Map.of(
                        "inline_keyboard",
                        List.of(
                                List.of(
                                        Map.of(
                                                "text",
                                                "✅ Duyệt",
                                                "callback_data",
                                                "ot_approve:" + request.getId()),
                                        Map.of(
                                                "text",
                                                "❌ Từ chối",
                                                "callback_data",
                                                "ot_reject:" + request.getId()))));

        Map<String, Object> payload =
                Map.of(
                        "chat_id", adminChatId,
                        "text", text,
                        "parse_mode", "Markdown",
                        "reply_markup", inlineKeyboard);

        try {
            String url = TELEGRAM_API + botToken + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                return result != null ? String.valueOf(result.get("message_id")) : null;
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram OT notification: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void sendAttendanceNotification(String message) {
        if (!isConfigured()) {
            log.warn("Telegram bot-token not configured. Skipping attendance notification.");
            return;
        }

        Map<String, Object> payload = Map.of("chat_id", adminChatId, "text", message);

        try {
            String url = TELEGRAM_API + botToken + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, entity, Map.class);
        } catch (Exception e) {
            log.error("Failed to send Telegram attendance notification: {}", e.getMessage());
        }
    }

    private boolean isConfigured() {
        return botToken != null
                && !botToken.isBlank()
                && adminChatId != null
                && !adminChatId.isBlank();
    }
}
