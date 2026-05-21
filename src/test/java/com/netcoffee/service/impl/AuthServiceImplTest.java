package com.netcoffee.service.impl;

import com.netcoffee.dto.request.LoginRequest;
import com.netcoffee.dto.response.AuthResponse;
import com.netcoffee.dto.response.SessionResponse;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.exception.InsufficientBalanceException;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.security.JwtTokenProvider;
import com.netcoffee.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock UserMapper userMapper;
    @Mock SessionService sessionService;
    @Mock AuthServiceImpl selfProxy;  // mock proxy để kiểm soát createSessionInNewTransaction

    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder,
                jwtTokenProvider, userMapper, sessionService);
        ReflectionTestUtils.setField(authService, "self", selfProxy);
    }

    // =========================================================================
    // login
    // =========================================================================

    @Nested
    @DisplayName("login")
    class LoginTest {

        @Test
        @DisplayName("Login bình thường → tạo session mới thành công, trả về token + session")
        void normalLogin_returnsTokenAndNewSession() {
            TUserEntity user = buildActiveUser(1L, "0901234567", "hashed");
            SessionResponse newSession = buildSession(100L, 1L, 5L);
            UserResponse userResp = UserResponse.builder().id(1L).build();
            LoginRequest req = buildLoginRequest("0901234567", "pass", 5L);

            when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
            when(jwtTokenProvider.generateToken(1L, "0901234567")).thenReturn("jwt-token");
            when(selfProxy.createSessionInNewTransaction(1L, 5L)).thenReturn(newSession);
            when(userMapper.toResponse(user)).thenReturn(userResp);

            AuthResponse result = authService.login(req);

            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getSession()).isEqualTo(newSession);
            assertThat(result.getSession().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Máy đang IN_USE bởi session cũ của user → reconnect trả về session cũ")
        void machineInUse_sameUserSameMachine_reconnectsToExistingSession() {
            TUserEntity user = buildActiveUser(1L, "0901234567", "hashed");
            SessionResponse existingSession = buildSession(99L, 1L, 5L); // machine 5L
            UserResponse userResp = UserResponse.builder().id(1L).build();
            LoginRequest req = buildLoginRequest("0901234567", "pass", 5L);

            when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
            when(jwtTokenProvider.generateToken(1L, "0901234567")).thenReturn("jwt-token");
            // Tạo session mới thất bại vì máy đang dùng
            when(selfProxy.createSessionInNewTransaction(1L, 5L))
                    .thenThrow(new IllegalStateException("Máy không khả dụng"));
            // Tìm thấy session cũ của user trên cùng máy
            when(sessionService.findActiveByUserId(1L)).thenReturn(existingSession);
            when(userMapper.toResponse(user)).thenReturn(userResp);

            AuthResponse result = authService.login(req);

            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getSession()).isEqualTo(existingSession);
            assertThat(result.getSession().getId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("Máy IN_USE nhưng session đang ở máy khác → trả về session = null")
        void machineInUse_sessionOnDifferentMachine_returnsNullSession() {
            TUserEntity user = buildActiveUser(1L, "0901234567", "hashed");
            SessionResponse sessionOnAnotherMachine = buildSession(88L, 1L, 99L); // machine 99L khác
            UserResponse userResp = UserResponse.builder().id(1L).build();
            LoginRequest req = buildLoginRequest("0901234567", "pass", 5L); // login vào máy 5L

            when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
            when(jwtTokenProvider.generateToken(1L, "0901234567")).thenReturn("jwt-token");
            when(selfProxy.createSessionInNewTransaction(1L, 5L))
                    .thenThrow(new IllegalStateException("Máy không khả dụng"));
            when(sessionService.findActiveByUserId(1L)).thenReturn(sessionOnAnotherMachine);
            when(userMapper.toResponse(user)).thenReturn(userResp);

            AuthResponse result = authService.login(req);

            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getSession()).isNull();
        }

        @Test
        @DisplayName("Không tìm thấy user → BadCredentialsException")
        void userNotFound_throwsBadCredentials() {
            when(userRepository.findByPhoneNumber("0999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(buildLoginRequest("0999", "p", 1L)))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Sai mật khẩu → BadCredentialsException")
        void wrongPassword_throwsBadCredentials() {
            TUserEntity user = buildActiveUser(1L, "0901234567", "hashed");
            when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(buildLoginRequest("0901234567", "wrong", 1L)))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Tài khoản bị khóa → BadCredentialsException")
        void lockedAccount_throwsBadCredentials() {
            TUserEntity user = TUserEntity.builder()
                    .id(1L).phoneNumber("0901234567").passwordHash("hashed")
                    .isActive(false).balance(BigDecimal.ZERO).build();
            when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(buildLoginRequest("0901234567", "pass", 1L)))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("khóa");
        }

        @Test
        @DisplayName("machineId = null → không thử reconnect, session = null")
        void nullMachineId_noSessionCreated() {
            TUserEntity user = buildActiveUser(1L, "0901234567", "hashed");
            UserResponse userResp = UserResponse.builder().id(1L).build();
            LoginRequest req = buildLoginRequest("0901234567", "pass", null);

            when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
            when(jwtTokenProvider.generateToken(1L, "0901234567")).thenReturn("jwt-token");
            when(userMapper.toResponse(user)).thenReturn(userResp);

            AuthResponse result = authService.login(req);

            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getSession()).isNull();
            verify(selfProxy, never()).createSessionInNewTransaction(any(), any());
        }

        @Test
        @DisplayName("Số dư không đủ 2000đ → InsufficientBalanceException propagate, không trả token")
        void insufficientBalance_throwsAndDoesNotReturnToken() {
            TUserEntity user = buildActiveUser(1L, "0901234567", "hashed");
            LoginRequest req = buildLoginRequest("0901234567", "pass", 5L);

            when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
            when(jwtTokenProvider.generateToken(1L, "0901234567")).thenReturn("jwt-token");
            when(selfProxy.createSessionInNewTransaction(1L, 5L))
                    .thenThrow(new InsufficientBalanceException("Số dư không đủ để mở máy. Cần tối thiểu 2000đ"));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Số dư không đủ");

            // Phải ném exception trước khi trả response → không tạo được AuthResponse
            verify(sessionService, never()).findActiveByUserId(any());
        }

        @Test
        @DisplayName("Máy IN_USE nhưng InsufficientBalance không bị nuốt → phân biệt đúng với IllegalState")
        void machineInUse_byIllegalState_stillReconnects() {
            TUserEntity user = buildActiveUser(1L, "0901234567", "hashed");
            SessionResponse existingSession = buildSession(99L, 1L, 5L);
            UserResponse userResp = UserResponse.builder().id(1L).build();
            LoginRequest req = buildLoginRequest("0901234567", "pass", 5L);

            when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
            when(jwtTokenProvider.generateToken(1L, "0901234567")).thenReturn("jwt-token");
            when(selfProxy.createSessionInNewTransaction(1L, 5L))
                    .thenThrow(new IllegalStateException("Máy không khả dụng"));
            when(sessionService.findActiveByUserId(1L)).thenReturn(existingSession);
            when(userMapper.toResponse(user)).thenReturn(userResp);

            AuthResponse result = authService.login(req);

            assertThat(result.getSession()).isEqualTo(existingSession);
        }
    }

    // =========================================================================
    // logout
    // =========================================================================

    @Nested
    @DisplayName("logout")
    class LogoutTest {

        @Test
        @DisplayName("Có session đang active → kết thúc session, không ném exception")
        void activeSession_endsSessionCleanly() {
            SessionResponse activeSession = buildSession(50L, 1L, 5L);
            when(sessionService.findActiveByUserId(1L)).thenReturn(activeSession);

            assertThatCode(() -> authService.logout(1L)).doesNotThrowAnyException();

            verify(sessionService).endSession(50L, 1L);
        }

        @Test
        @DisplayName("Không có session active → không gọi endSession, không ném exception")
        void noActiveSession_noOp() {
            when(sessionService.findActiveByUserId(1L)).thenReturn(null);

            assertThatCode(() -> authService.logout(1L)).doesNotThrowAnyException();

            verify(sessionService, never()).endSession(any(), any());
        }

        @Test
        @DisplayName("endSession ném exception → logout vẫn không propagate exception")
        void endSessionFails_logoutDoesNotThrow() {
            SessionResponse activeSession = buildSession(50L, 1L, 5L);
            when(sessionService.findActiveByUserId(1L)).thenReturn(activeSession);
            doThrow(new RuntimeException("DB lỗi")).when(sessionService).endSession(50L, 1L);

            assertThatCode(() -> authService.logout(1L)).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private TUserEntity buildActiveUser(Long id, String phone, String hash) {
        return TUserEntity.builder()
                .id(id).phoneNumber(phone).passwordHash(hash)
                .isActive(true).balance(new BigDecimal("10000")).build();
    }

    private SessionResponse buildSession(Long sessionId, Long userId, Long machineId) {
        return SessionResponse.builder()
                .id(sessionId).userId(userId).machineId(machineId).build();
    }

    private LoginRequest buildLoginRequest(String phone, String password, Long machineId) {
        LoginRequest req = new LoginRequest();
        req.setPhoneNumber(phone);
        req.setPassword(password);
        req.setMachineId(machineId);
        return req;
    }
}
