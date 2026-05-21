package com.netcoffee.service.impl;

import com.netcoffee.dto.request.LoginRequest;
import com.netcoffee.dto.request.RegisterRequest;
import com.netcoffee.dto.request.StartSessionRequest;
import com.netcoffee.dto.response.AuthResponse;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.security.JwtTokenProvider;
import com.netcoffee.service.AuthService;
import com.netcoffee.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    // Self-injection to allow Spring AOP to intercept @Transactional(REQUIRES_NEW)
    // Direct self-calls bypass the proxy and make REQUIRES_NEW a no-op.
    @Lazy
    @Autowired
    private AuthServiceImpl self;

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
        try {
            session = self.createSessionInNewTransaction(user.getId(), request.getMachineId());
            log.info("Auto session created: user={}, machine={}, session={}",
                    user.getId(), request.getMachineId(), session.getId());
        } catch (Exception e) {
            log.warn("Could not create session on login: {}", e.getMessage());
        }

        return AuthResponse.builder()
                .token(token)
                .user(userMapper.toResponse(user))
                .session(session)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionResponse createSessionInNewTransaction(Long userId, Long machineId) {
        StartSessionRequest sessionRequest = new StartSessionRequest();
        sessionRequest.setUserId(userId);
        sessionRequest.setMachineId(machineId);
        return sessionService.startSession(sessionRequest);
    }
}