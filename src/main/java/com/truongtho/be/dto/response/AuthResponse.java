package com.truongtho.be.dto.response;

import lombok.*;

@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    String token;
    String refreshToken;
    UserResponse user;
}