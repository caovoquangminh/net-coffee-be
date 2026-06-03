package com.netcoffee.constant;

import java.math.BigDecimal;

public final class AppConstant {

    private AppConstant() {}

    // QR Payment
    public static final int QR_EXPIRY_MINUTES = 5;
    public static final String QR_REFERENCE_PREFIX = "NETCOFFEE";

    // JWT
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final long JWT_EXPIRY_MS = 86_400_000L; // 24 hours

    // Session billing
    public static final int SESSION_BILLING_INTERVAL_SECONDS = 60; // tick mỗi 60s
    public static final BigDecimal SESSION_MINIMUM_CHARGE = new BigDecimal("2000"); // phí mở máy
    public static final int SESSION_MINIMUM_MINUTES = 15; // 2k = 15 phút
    public static final BigDecimal SESSION_PRICE_PER_HOUR = new BigDecimal("8000"); // giá mặc định

    // Session staleness — client phải gửi heartbeat mỗi 2 phút.
    // Session bị coi là stale (orphaned) nếu:
    //   - Đã có ít nhất 1 heartbeat nhưng heartbeat cuối > STALE_MINUTES trước
    //   - Hoặc chưa có heartbeat nào và startedAt > MAX_DURATION_HOURS trước (safety net cho phiên
    // cũ)
    public static final int SESSION_STALE_MINUTES = 30;
    public static final int SESSION_MAX_DURATION_HOURS = 12;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Feedback
    public static final int RATING_MIN = 1;
    public static final int RATING_MAX = 5;
}
