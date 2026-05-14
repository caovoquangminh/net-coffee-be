package com.netcoffee.utils;

import com.netcoffee.constant.AppConstant;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReferenceCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;

    // Pattern để tìm referenceCode trong nội dung CK
    // Ví dụ: "NETCOFFEE NC12AB34" -> match "NC12AB34"
    private static final Pattern CONTENT_PATTERN = Pattern.compile(
            AppConstant.QR_REFERENCE_PREFIX + "\\s+([A-Z0-9]{" + CODE_LENGTH + "})",
            Pattern.CASE_INSENSITIVE
    );

    private ReferenceCodeUtil() {}

    /**
     * Generate mã tham chiếu duy nhất
     * Format: NC + 6 ký tự random. Ví dụ: NCAB12XY
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder("NC");
        for (int i = 0; i < CODE_LENGTH - 2; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Tìm referenceCode trong nội dung chuyển khoản
     * Trả về null nếu không tìm thấy
     */
    public static String extractFromContent(String content) {
        if (content == null) return null;
        Matcher matcher = CONTENT_PATTERN.matcher(content.toUpperCase());
        return matcher.find() ? matcher.group(1) : null;
    }
}
