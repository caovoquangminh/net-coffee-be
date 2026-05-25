package com.netcoffee.service;

import com.netcoffee.dto.response.UserResponse;

import java.math.BigDecimal;

public interface CashTopUpService {

    /**
     * Nạp tiền mặt cho một tài khoản.
     *
     * @param targetUserId   ID tài khoản được nạp
     * @param amount         Số tiền
     * @param note           Ghi chú tuỳ chọn
     * @param performedById  ID nhân viên/admin thực hiện — lưu vào audit trail
     */
    UserResponse topUp(Long targetUserId, BigDecimal amount, String note, Long performedById);
}
