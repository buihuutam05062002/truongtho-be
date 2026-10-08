package com.truongtho.be.mapper;

import com.truongtho.be.dto.response.UserResponse;
import com.truongtho.be.entity.Role;
import com.truongtho.be.entity.Users;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserResponse toResponse(Users u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .avatarUrl(u.getAvatarUrl())
                .userStatus(u.getUserStatus())
                .roles(u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}