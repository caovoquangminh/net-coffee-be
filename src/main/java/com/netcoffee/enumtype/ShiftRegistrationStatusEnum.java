package com.netcoffee.enumtype;

public enum ShiftRegistrationStatusEnum {
    /** Nhân viên đã tự đăng ký (không cần admin duyệt). */
    REGISTERED,
    /** Nhân viên/admin đã hủy. */
    CANCELLED,
    /** Admin sắp ca chậm cho NV không đăng ký kịp cuối tuần. */
    ADMIN_ASSIGNED,
    /** Ca đã kết thúc và NV đã chấm công đầy đủ. */
    COMPLETED,
    /** Có đăng ký nhưng không đi làm. */
    ABSENT
}
