package com.netcoffee.service.impl;

import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.exception.InsufficientBalanceException;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByPhoneNumber(String phoneNumber) {
        TUserEntity user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TUserEntity getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + id));
    }

    @Override
    @Transactional
    public void topUp(Long userId, BigDecimal amount) {
        int updated = userRepository.increaseBalance(userId, amount);
        if (updated == 0) {
            throw new ResourceNotFoundException("Người dùng không tồn tại: " + userId);
        }
    }

    @Override
    @Transactional
    public boolean deduct(Long userId, BigDecimal amount) {
        int updated = userRepository.decreaseBalance(userId, amount);
        if (updated == 0) {
            // Kiểm tra user tồn tại không, nếu có thì do balance không đủ
            if (!userRepository.existsById(userId)) {
                throw new ResourceNotFoundException("Người dùng không tồn tại: " + userId);
            }
            throw new InsufficientBalanceException("Số dư không đủ");
        }
        return true;
    }
}
