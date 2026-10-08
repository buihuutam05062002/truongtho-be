package com.truongtho.be.service;

import com.truongtho.be.dto.response.AuthResponse;
import com.truongtho.be.entity.RefreshToken;
import com.truongtho.be.entity.Users;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.mapper.UserMapper;
import com.truongtho.be.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshDays;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public AuthResponse issue(Users user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .refreshToken(createRefreshToken(user))
                .user(userMapper.toResponse(user))
                .build();
    }
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse refresh(String rawToken) {
        RefreshToken old = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new AppException("Refresh token không hợp lệ"));

        if (old.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(old.getUser().getId());
            throw new AppException("Refresh token đã bị thu hồi, vui lòng đăng nhập lại");
        }
        if (old.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        Users user = old.getUser();
        checkUserStatus(user);

        old.setRevoked(true);
        return issue(user);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public void logoutAll(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }


    private String createRefreshToken(Users user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash(raw));
        rt.setExpiresAt(Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);

        return raw;
    }

    private String hash(String raw) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void checkUserStatus(Users user) {
        switch (user.getUserStatus()) {
            case LOCKED -> throw new AppException("Tài khoản đã bị khóa");
            case DELETED -> throw new AppException("Tài khoản không tồn tại");
            case INACTIVE -> throw new AppException("Tài khoản không hoạt động");
            case PENDING -> throw new AppException("Tài khoản chưa được kích hoạt");
            default -> { }
        }
    }
}