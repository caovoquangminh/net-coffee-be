package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.service.TelegramCallbackHandler;
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

    private final TelegramCallbackHandler callbackHandler;
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

        String resultText;
        try {
            resultText = callbackHandler.handle(data);
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
}
