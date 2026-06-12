package com.netcoffee.enumtype;

public enum AttendanceStatusEnum {
    ON_TIME,
    LATE,
    EARLY_LEAVE,
    ABSENT,
    /** Hệ thống tự check-out vì NV quên check-out (chốt tại cuối ca). */
    AUTO_CHECKOUT
}
