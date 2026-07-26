package com.tadaktadak.eunggeubi.domain.auth.service;

import com.tadaktadak.eunggeubi.domain.auth.dto.LoginRequest;
import com.tadaktadak.eunggeubi.domain.auth.dto.LoginResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.SignupRequest;
import com.tadaktadak.eunggeubi.domain.auth.dto.SignupResponse;
import com.tadaktadak.eunggeubi.domain.auth.entity.RefreshToken;
import com.tadaktadak.eunggeubi.domain.auth.repository.RefreshTokenRepository;
import com.tadaktadak.eunggeubi.domain.user.entity.TermsAgreement;
import com.tadaktadak.eunggeubi.domain.user.entity.TermsType;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.TermsAgreementRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TERMS_VERSION = "1.0";
    private static final int GUARDIAN_CONSENT_AGE = 14;

    private final UserRepository userRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 2. 만 14세 미만 판별 → 보호자 동의 필요 여부 & 회원 상태 결정
        boolean guardianConsentRequired =
                Period.between(request.birthDate(), LocalDate.now()).getYears() < GUARDIAN_CONSENT_AGE;
        UserStatus status = guardianConsentRequired ? UserStatus.PENDING : UserStatus.ACTIVE;

        // 3. 회원 저장 (비밀번호는 BCrypt로 암호화)
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .birthDate(request.birthDate())
                .gender(request.gender())
                .address(request.address())
                .status(status)
                .build();
        userRepository.save(user);

        // 4. 약관 동의 이력 저장 (필수 3종)
        saveTermsAgreements(user.getId());

        return new SignupResponse(user.getId(), status, guardianConsentRequired);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 이메일로 회원 조회 (없으면 실패 — 뭐가 틀렸는지 노출 안 함)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 2. 비밀번호 확인 (소셜 전용 계정은 password가 null이라 자동 실패)
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 3. 탈퇴 회원 차단
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }

        // 4. 토큰 발급 (access + refresh)
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 5. refresh 토큰은 해시로 DB 저장 (재발급/로그아웃 관리용)
        saveRefreshToken(user.getId(), refreshToken);

        return new LoginResponse(user.getId(), accessToken, refreshToken, "Bearer");
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(refreshToken))
                .issuedAt(LocalDateTime.now())
                .expiresAt(jwtProvider.getExpiration(refreshToken))
                .build());
    }

    // refresh 토큰을 SHA-256으로 해시 (DB엔 원본 대신 해시만 저장)
    private String hashToken(String token) {
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

    private void saveTermsAgreements(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        for (TermsType type : new TermsType[]{TermsType.SERVICE, TermsType.PRIVACY, TermsType.SENSITIVE_INFO}) {
            termsAgreementRepository.save(TermsAgreement.builder()
                    .userId(userId)
                    .termsType(type)
                    .termsVersion(TERMS_VERSION)
                    .agreed(true)
                    .agreedAt(now)
                    .build());
        }
    }
}