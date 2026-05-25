package com.netcoffee.service.impl;

import com.netcoffee.dto.request.LoginRequest;
import com.netcoffee.dto.request.RegisterRequest;
import com.netcoffee.dto.response.AuthResponse;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.exception.InsufficientBalanceException;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.security.JwtTokenProvider;
import com.netcoffee.service.AuthService;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final SessionService sessionService;

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
                .role(UserRoleEnum.CUSTOMER)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        TUserEntity user = userRepository
                .findByPhoneNumber(request.getPhoneNumber())
                .orElse(null);

        if (user == null) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Tài khoản đã bị khóa, vui lòng liên hệ nhân viên");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getPhoneNumber());

        SessionResponse session = null;
        if (request.getMachineId() == null) {
            return AuthResponse.builder()
                    .token(token).user(userMapper.toResponse(user)).session(null).build();
        }
        try {
            session = sessionService.getOrStartSession(user.getId(), request.getMachineId());
            if (session != null) {
                log.info("Session ready: user={}, machine={}, session={}",
                        user.getId(), request.getMachineId(), session.getId());
            }
        } catch (InsufficientBalanceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not get or start session on login: {}", e.getMessage());
        }

        return AuthResponse.builder()
                .token(token)
                .user(userMapper.toResponse(user))
                .session(session)
                .build();
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        SessionResponse activeSession = sessionService.findActiveByUserId(userId);
        if (activeSession == null) {
            log.info("Logout: user={} has no active session", userId);
            return;
        }
        try {
            sessionService.endSession(activeSession.getId(), userId);
            log.info("Logout: user={} ended session={}", userId, activeSession.getId());
        } catch (Exception e) {
            log.warn("Could not end session on logout: user={}, session={}, reason={}",
                    userId, activeSession.getId(), e.getMessage());
        }
    }

}