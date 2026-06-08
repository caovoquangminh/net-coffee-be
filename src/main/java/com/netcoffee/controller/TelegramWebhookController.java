package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.service.OvertimeService;
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
        if (data == null) {
            return ResponseEntity.ok().build();
        }

        log.info("Telegram callback_query data: {}", data);

        try {
            if (data.startsWith("ot_approve:")) {
                Long requestId = Long.parseLong(data.substring("ot_approve:".length()));
                overtimeService.approveOvertime(requestId, null);
                log.info("Approved OT request {} via Telegram", requestId);
            } else if (data.startsWith("ot_reject:")) {
                Long requestId = Long.parseLong(data.substring("ot_reject:".length()));
                overtimeService.rejectOvertime(requestId);
                log.info("Rejected OT request {} via Telegram", requestId);
            }
        } catch (Exception e) {
            log.error("Error processing Telegram callback: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
