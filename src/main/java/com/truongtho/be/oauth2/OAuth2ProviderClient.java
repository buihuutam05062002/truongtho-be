package com.truongtho.be.oauth2;


import com.truongtho.be.enums.AuthProvider;

public interface OAuth2ProviderClient {
    AuthProvider getProvider();

    OAuth2UserInfo fetchUserInfo(String credential);
}
