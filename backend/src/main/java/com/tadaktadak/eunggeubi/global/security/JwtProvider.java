package com.tadaktadak.eunggeubi.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "tokenType";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String TYPE_CONSENT = "consent";

    // 보호자 동의 링크 유효기간(3일)과 맞춘다 - 그 안에 동의가 안 되면 어차피 링크도 만료된다.
    private static final long CONSENT_TOKEN_VALIDITY_MS = 3L * 24 * 60 * 60 * 1000;

    private final SecretKey key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    // access 토큰 생성
    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenValidityMs, TYPE_ACCESS);
    }

    // refresh 토큰 생성
    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenValidityMs, TYPE_REFRESH);
    }
    // 보호자 동의 전용 토큰. 가입 응답으로 내려주고, 보호자 정보 입력/상태 조회에만 쓴다.
    // tokenType 이 access 가 아니라서 JwtAuthenticationFilter 는 이 토큰을 인증으로 쳐주지 않는다.
    public String createConsentToken(Long userId) {
        return createToken(userId, CONSENT_TOKEN_VALIDITY_MS, TYPE_CONSENT);
    }

    // 보호자 동의 토큰인지 검사 (서명·만료·종류)
    public boolean isConsentToken(String token) {
        try {
            return TYPE_CONSENT.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String createToken(Long userId, long validityMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString()) // 매번 고유(jti) → 같은 초에 만들어도 토큰이 달라짐
                .subject(String.valueOf(userId)) // 토큰 주인 = 회원 id
                .claim(CLAIM_TYPE, tokenType) //access/refresh 구분
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 토큰에서 회원 id 추출 (서명·만료 검증 포함)
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    // 토큰의 만료 시각을 LocalDateTime으로 반환 (refresh 토큰 DB 저장용)
    public LocalDateTime getExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    // access 토큰인지 검사 (서명·만료·종류)
    public boolean isAccessToken(String token) {
        try {
            return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // refresh 토큰인지 검사 (서명·만료·종류)
    public boolean isRefreshToken(String token) {
        try {
            return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}