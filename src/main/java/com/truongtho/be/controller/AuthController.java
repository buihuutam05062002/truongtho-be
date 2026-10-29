package com.truongtho.be.controller;

import com.truongtho.be.dto.request.LoginRequest;
import com.truongtho.be.dto.request.OAuth2LoginRequest;
import com.truongtho.be.dto.request.RefreshRequest;
import com.truongtho.be.dto.request.RegisterRequest;
import com.truongtho.be.dto.response.AuthResponse;
import com.truongtho.be.dto.response.UserResponse;
import com.truongtho.be.enums.AuthProvider;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.exception.ErrorBody;
import com.truongtho.be.service.AuthService;
import com.truongtho.be.service.OAuth2LoginService;
import com.truongtho.be.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String RT = "refresh_token";

    private final TokenService tokenService;

    private final AuthService authService;
    private final OAuth2LoginService oauth2LoginService;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;
    @Value("${app.cookie.path}")
    private String cookiePath;
    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshDays;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ok(authService.login(request));
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
        return ok(oauth2LoginService.login(authProvider, request.getCredential()));
    }




    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = RT, required = false) String rt) {
        if(rt != null){
            tokenService.logout(rt);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,cookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = RT, required = false) String rt) {
        try{
            if(rt == null){
                throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
            }
            return ok(tokenService.refresh(rt));
        }catch(AppException e){
            return ResponseEntity.status(e.getCode().getStatus())
                    .header(HttpHeaders.SET_COOKIE, cookie("",Duration.ZERO).toString())
                    .body(new ErrorBody(new ErrorBody.Err(e.getCode().getCode(),e.getMessage(),null)))
                    ;
        }
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth){
        return authService.me(auth.getName());
    }

    private ResponseEntity<AuthResponse> ok(AuthResponse a){
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(a.getRefreshToken(),Duration.ofDays(refreshDays)).toString())
                .body(AuthResponse.builder()
                        .token(a.getToken()).user(a.getUser()).build());
    }


    private ResponseCookie cookie(String value, Duration maxAge){
        return ResponseCookie.from(RT,value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(cookiePath)
                .maxAge(maxAge)
                .build();
    }
}