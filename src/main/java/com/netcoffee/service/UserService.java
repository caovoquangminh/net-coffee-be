package com.netcoffee.service;

import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;

import java.math.BigDecimal;

public interface UserService {

    UserResponse findById(Long id);

    UserResponse findByPhoneNumber(String phoneNumber);

    TUserEntity getEntityById(Long id);

    void topUp(Long userId, BigDecimal amount);

    /**
     * Trả về false nếu số dư không đủ
     */
    boolean deduct(Long userId, BigDecimal amount);
}
