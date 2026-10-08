package com.truongtho.be.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    String token;
    String refreshToken;
    UserResponse user;
}