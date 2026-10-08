package com.truongtho.be.dto.response;

import com.truongtho.be.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class UserResponse {
    String id;
    String fullName;
    String email;
    String phoneNumber;
    String avatarUrl;
    UserStatus userStatus;
    Set<String> roles;
}