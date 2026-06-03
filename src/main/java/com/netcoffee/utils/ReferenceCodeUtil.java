package com.netcoffee.utils;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReferenceCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // NC + 6 ký tự = 8 ký tự total
    private static final int RANDOM_LENGTH = 6;

    /** Match: NC123456 nap tien NC123456 abc NCABC123 xyz */
    private static final Pattern CONTENT_PATTERN =
            Pattern.compile("(NC[A-Z0-9]{6})", Pattern.CASE_INSENSITIVE);

    private ReferenceCodeUtil() {}

    public static String generate() {
        StringBuilder sb = new StringBuilder("NC");

        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }

        return sb.toString();
    }

    public static String extractFromContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Matcher matcher = CONTENT_PATTERN.matcher(content.toUpperCase());

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}
