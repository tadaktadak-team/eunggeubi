package com.tadaktadak.eunggeubi.domain.auth.service;

import com.tadaktadak.eunggeubi.domain.auth.entity.RefreshToken;
import com.tadaktadak.eunggeubi.domain.auth.repository.RefreshTokenRepository;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//refresh 토큰의 발급/폐기
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    //회원의 아직 살아있는 refresh 토큰을 전부 폐기
    @Transactional
    public void revokeAll(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.revoke(now));
    }

    //새 refresh 토큰을 발급해 저장하고 원본을 반환
    @Transactional
    public String issue(Long userId) {
        String refreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(refreshToken))
                .issuedAt(LocalDateTime.now())
                .expiresAt(jwtProvider.getExpiration(refreshToken))
                .build());
        return refreshToken;
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}