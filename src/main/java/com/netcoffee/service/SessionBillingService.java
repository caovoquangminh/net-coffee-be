package com.netcoffee.service;

public interface SessionBillingService {

    /**
     * Trừ phí mở máy tối thiểu khi bắt đầu session.
     * Phí này không hoàn lại dù user logout sớm.
     */
    void chargeMinimumFee(Long userId, Long sessionId);

    /**
     * Scheduled tick — chạy mỗi phút để trừ tiền realtime.
     * Chỉ trừ sau khi đã qua thời gian minimum.
     * Force end session nếu balance <= 0.
     */
    void billingTick();
}