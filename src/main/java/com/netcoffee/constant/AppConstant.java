package com.netcoffee.constant;

public final class AppConstant {

    private AppConstant() {}

    // QR Payment
    public static final int QR_EXPIRY_MINUTES = 5;
    public static final String QR_REFERENCE_PREFIX = "NETCOFFEE";

    // JWT
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final long JWT_EXPIRY_MS = 86_400_000L; // 24 hours

    // Session
    public static final int SESSION_BILLING_INTERVAL_SECONDS = 60; // trừ tiền mỗi 60s

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Feedback
    public static final int RATING_MIN = 1;
    public static final int RATING_MAX = 5;
}