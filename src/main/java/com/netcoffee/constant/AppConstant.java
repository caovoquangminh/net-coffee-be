package com.netcoffee.constant;

import java.math.BigDecimal;
import java.time.ZoneId;

public final class AppConstant {

    private AppConstant() {}

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final int QR_EXPIRY_MINUTES = 5;
    public static final String QR_REFERENCE_PREFIX = "NETCOFFEE";

    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final long JWT_EXPIRY_MS = 86_400_000L;

    public static final int SESSION_BILLING_INTERVAL_SECONDS = 60;
    public static final BigDecimal SESSION_MINIMUM_CHARGE = new BigDecimal("2000");
    public static final int SESSION_MINIMUM_MINUTES = 15;
    public static final BigDecimal SESSION_PRICE_PER_HOUR = new BigDecimal("8000");

    // Session staleness — client phải gửi heartbeat mỗi 2 phút.
    // Session bị coi là stale (orphaned) nếu:
    //   - Đã có ít nhất 1 heartbeat nhưng heartbeat cuối > STALE_MINUTES trước
    //   - Hoặc chưa có heartbeat nào và startedAt > MAX_DURATION_HOURS trước (safety net cho phiên
    // cũ)
    public static final int SESSION_STALE_MINUTES = 30;
    public static final int SESSION_MAX_DURATION_HOURS = 12;

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final int RATING_MIN = 1;
    public static final int RATING_MAX = 5;
}
