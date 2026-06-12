package com.netcoffee.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Định dạng ngày/giờ kiểu VN (ngày/tháng/năm) cho thông báo Telegram. */
public final class DateFmt {

    private DateFmt() {}

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Ngày dạng dd/MM/yyyy. */
    public static String date(LocalDate d) {
        return d == null ? "" : d.format(DATE);
    }

    /** Giờ dạng HH:mm. */
    public static String time(LocalDateTime t) {
        return t == null ? "" : t.format(TIME);
    }

    /** Ngày giờ dạng dd/MM/yyyy HH:mm. */
    public static String dateTime(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME);
    }
}
