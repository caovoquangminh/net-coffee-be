package com.netcoffee.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SessionBillingService {

    /**
     * Trừ phí mở máy tối thiểu khi bắt đầu session.
     * Đồng thời set lastBilledAt = startedAt + SESSION_MINIMUM_MINUTES để
     * billing tick biết điểm bắt đầu charge tiếp theo.
     */
    void chargeMinimumFee(Long userId, Long sessionId);

    /**
     * Scheduled tick — chạy mỗi phút để trừ tiền theo giây thực từ lastBilledAt.
     * Force end session nếu balance <= 0.
     */
    void billingTick();

    /**
     * Kết toán cuối phiên — charge phần lẻ từ lastBilledAt đến endedAt.
     * Gọi ngay trước khi đổi session status sang ENDED/FORCE_ENDED.
     * Không throw nếu balance = 0 (charge tối đa bằng số dư hiện có).
     */
    void chargeFinalBill(Long userId, Long sessionId, LocalDateTime lastBilledAt,
                         LocalDateTime endedAt, BigDecimal pricePerHour);
}
