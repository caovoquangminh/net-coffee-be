package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.ChangePasswordRequest;
import com.netcoffee.dto.request.CreateCustomerRequest;
import com.netcoffee.dto.request.ResetPasswordRequest;
import com.netcoffee.dto.request.UpdateProfileRequest;
import com.netcoffee.dto.request.UpdateStaffProfileRequest;
import com.netcoffee.dto.request.UpdateUserRequest;
import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.mapper.UserMapper;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.UserManagementService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse adminUpdateUser(Long userId, UpdateUserRequest request) {
        TUserEntity user = findOrThrow(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().isBlank() ? null : request.getFullName().trim());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        // ADMIN role cannot be assigned via this endpoint
        if (request.getRole() != null && request.getRole() != UserRoleEnum.ADMIN) {
            user.setRole(request.getRole());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void adminResetPassword(Long userId, ResetPasswordRequest request) {
        TUserEntity user = findOrThrow(userId);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse updateStaffProfile(Long userId, UpdateStaffProfileRequest request) {
        TUserEntity user = findOrThrow(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().isBlank() ? null : request.getFullName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(
                    request.getAvatarUrl().isBlank() ? null : request.getAvatarUrl().trim());
        }
        if (request.getStaffAddress() != null) {
            user.setStaffAddress(
                    request.getStaffAddress().isBlank() ? null : request.getStaffAddress().trim());
        }
        if (request.getIdCard() != null) {
            user.setIdCard(request.getIdCard().isBlank() ? null : request.getIdCard().trim());
        }
        if (request.getStaffEmail() != null) {
            user.setStaffEmail(
                    request.getStaffEmail().isBlank() ? null : request.getStaffEmail().trim());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getStartDate() != null) {
            user.setStartDate(request.getStartDate());
        }
        if (request.getHourlyWage() != null) {
            user.setHourlyWage(request.getHourlyWage());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void softDeleteUser(Long userId, Long adminId) {
        if (userId.equals(adminId)) {
            throw new IllegalArgumentException("Không thể xóa chính mình");
        }
        TUserEntity user = findOrThrow(userId);
        user.setDeletedAt(LocalDateTime.now(AppConstant.VN_ZONE));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        TUserEntity user = findOrThrow(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().isBlank() ? null : request.getFullName().trim());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changeMyPassword(Long userId, ChangePasswordRequest request) {
        TUserEntity user = findOrThrow(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu cũ không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse createCustomer(CreateCustomerRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký");
        }
        UserRoleEnum role =
                (request.getRole() != null && request.getRole() != UserRoleEnum.ADMIN)
                        ? request.getRole()
                        : UserRoleEnum.CUSTOMER;
        TUserEntity user =
                TUserEntity.builder()
                        .phoneNumber(request.getPhoneNumber())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .fullName(
                                request.getFullName() != null ? request.getFullName().trim() : null)
                        .isActive(true)
                        .role(role)
                        .build();
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.netcoffee.dto.response.UserResponse> searchCustomers(String phone) {
        if (phone == null || phone.length() < 3) {
            return java.util.List.of();
        }
        return userRepository
                .findByRoleAndPhoneNumberContainingOrderByCreatedAtDesc(
                        com.netcoffee.enumtype.UserRoleEnum.CUSTOMER, phone)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    private TUserEntity findOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Người dùng không tồn tại: " + userId));
    }
}
