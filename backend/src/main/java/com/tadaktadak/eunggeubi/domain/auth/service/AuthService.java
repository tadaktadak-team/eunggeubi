package com.tadaktadak.eunggeubi.domain.auth.service;
import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
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
    private final PhoneVerificationService phoneVerificationService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        // 1-2. 휴대폰 인증 완료된 번호인지 확인 (인증 안 된 번호는 가입 불가)
        phoneVerificationService.ensureVerified(request.phone(), Purpose.SIGNUP);

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

        // 4. 미성년자 보호자 동의 대기 회원 차단 (동의 완료 전엔 로그인 불가)
        if (user.getStatus() == UserStatus.PENDING) {
            throw new IllegalArgumentException("보호자 동의가 완료되지 않은 계정입니다.");
        }


        // 5. 토큰 발급 (access + refresh)
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = refreshTokenService.issue(user.getId());

        return new LoginResponse(user.getId(), accessToken, refreshToken, "Bearer");
    }
    @Transactional
    public LoginResponse reissue(String refreshToken) {
        // 1. 토큰 자체 유효성 (서명·만료)
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. DB에 저장된 토큰인지 확인 (해시로 조회)
        RefreshToken saved = refreshTokenRepository.findByTokenHash(refreshTokenService.hashToken(refreshToken))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));

        // 3. 이미 폐기(로그아웃/재사용)된 토큰이면 거부
        if (saved.getRevokedAt() != null) {
            throw new IllegalArgumentException("이미 로그아웃되었거나 만료된 토큰입니다.");
        }

        // 4. 기존 토큰 폐기 (재발급 시 회전 — 한 번 쓴 refresh는 무효화)
        saved.revoke(LocalDateTime.now());

        // 5. 새 access + refresh 발급
        Long userId = jwtProvider.getUserId(refreshToken);
        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = refreshTokenService.issue(userId);

        return new LoginResponse(userId, newAccessToken, newRefreshToken, "Bearer");
    }

    @Transactional
    public void logout(String refreshToken) {
        // 해당 refresh 토큰 폐기 (없거나 이미 폐기여도 조용히 성공 — 멱등)
        refreshTokenRepository.findByTokenHash(refreshTokenService.hashToken(refreshToken))
                .ifPresent(token -> token.revoke(LocalDateTime.now()));
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
