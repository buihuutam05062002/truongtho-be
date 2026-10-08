package com.truongtho.be.oauth2;

import com.truongtho.be.enums.AuthProvider;
import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class OAuth2UserInfo {
    AuthProvider provider;
    String providerId;
    String email;
    boolean emailVerified;
    String name;
    String avatarUrl;
}
