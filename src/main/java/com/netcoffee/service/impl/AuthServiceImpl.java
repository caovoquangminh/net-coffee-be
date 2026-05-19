package com.netcoffee.service.impl;

import com.netcoffee.dto.request.LoginRequest;
import com.netcoffee.dto.request.RegisterRequest;
import com.netcoffee.dto.response.AuthResponse;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.security.JwtTokenProvider;
import com.netcoffee.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    // Message chung — không tiết lộ tài khoản có tồn tại hay không
    private static final String INVALID_CREDENTIALS = "Số điện thoại hoặc mật khẩu không đúng";

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký");
        }

        TUserEntity user = TUserEntity.builder()
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Dùng orElse(null) thay vì orElseThrow để không tiết lộ
        // tài khoản có tồn tại hay không
        TUserEntity user = userRepository
                .findByPhoneNumber(request.getPhoneNumber())
                .orElse(null);

        // Tài khoản không tồn tại → cùng message với sai mật khẩu
        if (user == null) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        // Tài khoản bị khóa → message riêng vì cần user biết lý do
        if (!user.getIsActive()) {
            throw new BadCredentialsException("Tài khoản đã bị khóa, vui lòng liên hệ nhân viên");
        }

        // Sai mật khẩu → cùng message với không tồn tại
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getPhoneNumber());

        return AuthResponse.builder()
                .token(token)
                .user(userMapper.toResponse(user))
                .build();
    }
}