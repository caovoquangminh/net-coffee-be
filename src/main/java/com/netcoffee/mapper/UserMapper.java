package com.netcoffee.mapper;

import com.netcoffee.dto.response.UserResponse;
import com.netcoffee.entity.TUserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(TUserEntity entity) {
        if (entity == null) return null;
        return UserResponse.builder()
                .id(entity.getId())
                .phoneNumber(entity.getPhoneNumber())
                .fullName(entity.getFullName())
                .avatarUrl(entity.getAvatarUrl())
                .balance(entity.getBalance())
                .totalSpent(entity.getTotalSpent())
                .isActive(entity.getIsActive())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
