package com.truongtho.be.controller;

import com.truongtho.be.dto.request.LoginRequest;
import com.truongtho.be.dto.request.OAuth2LoginRequest;
import com.truongtho.be.dto.request.RefreshRequest;
import com.truongtho.be.dto.request.RegisterRequest;
import com.truongtho.be.dto.response.AuthResponse;
import com.truongtho.be.enums.AuthProvider;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.service.AuthService;
import com.truongtho.be.service.OAuth2LoginService;
import com.truongtho.be.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final TokenService tokenService;

    private final AuthService authService;
    private final OAuth2LoginService oauth2LoginService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/oauth2/{provider}")
    public ResponseEntity<AuthResponse> oauth2Login(@PathVariable String provider,
                                                    @Valid @RequestBody OAuth2LoginRequest request) {
        AuthProvider authProvider;
        try {
            authProvider = AuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("Provider không được hỗ trợ");
        }
        return ResponseEntity.ok(oauth2LoginService.login(authProvider, request.getCredential()));
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(tokenService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        tokenService.logout(req.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}