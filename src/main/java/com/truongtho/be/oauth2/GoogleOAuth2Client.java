package com.truongtho.be.oauth2;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.truongtho.be.enums.AuthProvider;
import com.truongtho.be.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

import java.security.GeneralSecurityException;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2Client implements OAuth2ProviderClient{

    private final GoogleIdTokenVerifier verifier;

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuth2UserInfo fetchUserInfo(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new AppException("Token Google không hợp lệ");
            }
            GoogleIdToken.Payload p = idToken.getPayload();

            return OAuth2UserInfo.builder()
                    .provider(AuthProvider.GOOGLE)
                    .providerId(p.getSubject())
                    .email(p.getEmail())
                    .emailVerified(Boolean.TRUE.equals(p.getEmailVerified()))
                    .name((String) p.get("name"))
                    .avatarUrl((String) p.get("picture"))
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new AppException("Không thể xác thực token Google");
        }
    }
}
