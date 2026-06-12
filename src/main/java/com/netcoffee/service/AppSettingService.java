package com.netcoffee.service;

import java.math.BigDecimal;
import java.util.Map;

/** Đọc/ghi cấu hình hệ thống (key-value): Telegram chat id, tham số thưởng/phạt. */
public interface AppSettingService {

    /** Khóa cấu hình. */
    String TELEGRAM_CHAT_ID = "telegram_chat_id";

    String TELEGRAM_BOT_TOKEN = "telegram_bot_token";
    String ATTENDANCE_BONUS = "attendance_bonus";
    String LATE_PENALTY_PER_MINUTE = "late_penalty_per_minute";
    String ABSENT_PENALTY = "absent_penalty";
    String OVERTIME_MULTIPLIER = "overtime_multiplier";

    String get(String key, String defaultValue);

    BigDecimal getDecimal(String key, BigDecimal defaultValue);

    void set(String key, String value);

    /** Toàn bộ cấu hình (cho màn admin). */
    Map<String, String> getAll();
}
