package com.netcoffee.scheduler;

import com.netcoffee.service.TelegramCallbackHandler;
import com.netcoffee.service.TelegramService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Long-polling Telegram (getUpdates) để nhận cú nhấn nút Duyệt/Từ chối khi backend chạy local
 * (không có webhook public). Production có URL công khai có thể tắt bằng {@code
 * app.telegram.polling-enabled=false} và dùng webhook.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramPoller {

    private static final int LONG_POLL_TIMEOUT_SECONDS = 25;

    private final TelegramService telegramService;
    private final TelegramCallbackHandler callbackHandler;

    @Value("${app.telegram.polling-enabled:true}")
    private boolean pollingEnabled;

    private volatile boolean running = true;
    private long offset = 0;
    private boolean webhookCleared = false;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!pollingEnabled) {
            log.info("Telegram long-polling: TẮT (app.telegram.polling-enabled=false)");
            return;
        }
        Thread thread = new Thread(this::loop, "telegram-poller");
        thread.setDaemon(true);
        thread.start();
        log.info("Telegram long-polling: BẬT");
    }

    private void loop() {
        while (running) {
            try {
                if (!telegramService.isConfigured()) {
                    sleep(10_000); // chưa cấu hình bot → chờ admin nhập ở Settings rồi thử lại
                    continue;
                }
                if (!webhookCleared) {
                    telegramService.deleteWebhook(); // tránh xung đột getUpdates vs webhook
                    webhookCleared = true;
                }
                List<Map<String, Object>> updates =
                        telegramService.getUpdates(offset, LONG_POLL_TIMEOUT_SECONDS);
                for (Map<String, Object> update : updates) {
                    Object updateId = update.get("update_id");
                    if (updateId instanceof Number n) {
                        offset = n.longValue() + 1;
                    }
                    handleUpdate(update);
                }
            } catch (Exception e) {
                log.warn("Telegram poll loop lỗi: {}", e.getMessage());
                sleep(5_000);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleUpdate(Map<String, Object> update) {
        Map<String, Object> cb = (Map<String, Object>) update.get("callback_query");
        if (cb == null) {
            return;
        }
        String data = (String) cb.get("data");
        String callbackId = (String) cb.get("id");
        String result;
        try {
            result = callbackHandler.handle(data);
        } catch (Exception e) {
            log.error("Lỗi xử lý callback Telegram: {}", e.getMessage());
            result = "Lỗi: " + e.getMessage();
        }
        telegramService.answerCallback(callbackId, result);

        // Sửa message gốc: nối kết quả + gỡ nút để khỏi bấm lại.
        Map<String, Object> message = (Map<String, Object>) cb.get("message");
        if (message != null) {
            Object messageId = message.get("message_id");
            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
            String originalText = (String) message.get("text");
            if (messageId instanceof Number mid && chat != null && chat.get("id") != null) {
                String newText = (originalText != null ? originalText : "") + "\n\n— " + result;
                telegramService.editMessageText(
                        String.valueOf(chat.get("id")), mid.longValue(), newText);
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
