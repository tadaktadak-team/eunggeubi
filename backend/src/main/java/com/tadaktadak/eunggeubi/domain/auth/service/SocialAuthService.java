package com.tadaktadak.eunggeubi.domain.auth.service;

import com.tadaktadak.eunggeubi.domain.auth.dto.LoginResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.SocialProfile;
import com.tadaktadak.eunggeubi.domain.auth.entity.Provider;
import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import com.tadaktadak.eunggeubi.domain.auth.entity.SocialAccount;
import com.tadaktadak.eunggeubi.domain.auth.repository.SocialAccountRepository;
import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import com.tadaktadak.eunggeubi.domain.user.entity.TermsAgreement;
import com.tadaktadak.eunggeubi.domain.user.entity.TermsType;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.TermsAgreementRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private static final String TERMS_VERSION = "1.0";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final PhoneVerificationService phoneVerificationService;

    /**
     * 기존 회원이면 로그인 결과를 반환, 완전 신규면 empty 반환(→ 약관/정보 입력 화면 필요).
     */
    @Transactional
    public Optional<LoginResponse> loginIfExisting(Provider provider, SocialProfile profile) {
        SocialAccount linked = socialAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .orElse(null);
        if (linked != null) {
            User user = userRepository.findById(linked.getUserId())
                    .orElseThrow(() -> new IllegalStateException("연동된 회원을 찾을 수 없습니다."));
            return Optional.of(issueTokens(requireActive(user)));
        }

        if (profile.email() != null) {
            User existing = userRepository.findByEmail(profile.email()).orElse(null);
            if (existing != null) {
                linkSocialAccount(provider, profile, existing.getId());
                return Optional.of(issueTokens(requireActive(existing)));
            }
        }

        return Optional.empty();
    }

    /**
     * 약관 동의(+부족 정보 입력)를 마친 신규 소셜회원을 생성하고 로그인시킨다.
     * phone/birthDate/gender 는 제공자가 안 준 필수정보를 앱에서 받아 채우는 값(없으면 null).
     */
    @Transactional
    public LoginResponse completeSignup(Provider provider, SocialProfile profile,
                                        String phone, LocalDate birthDate, Gender gender) {
        if (profile.email() == null) {
            throw new IllegalArgumentException("소셜 계정에서 이메일을 받지 못했습니다.");
        }
        // 중복 클릭 등으로 그 사이 이미 만들어졌으면 기존 회원을 쓴다
        User user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> createUser(profile, phone, birthDate, gender));
        linkSocialAccount(provider, profile, user.getId());
        return issueTokens(requireActive(user));
    }

    // 소셜 프로필 + 앱 입력값으로 새 회원 생성 (비밀번호 없음, 바로 ACTIVE) + 약관 기록
    private User createUser(SocialProfile profile, String reqPhone, LocalDate reqBirthDate, Gender reqGender) {
        // 제공자가 준 값 우선, 없으면 앱에서 입력받은 값으로 채운다
        String phone = profile.phone() != null ? profile.phone() : reqPhone;
        LocalDate birthDate = profile.birthDate() != null ? profile.birthDate() : reqBirthDate;
        Gender gender = (profile.gender() != null && profile.gender() != Gender.NONE)
                ? profile.gender()
                : (reqGender != null ? reqGender : Gender.NONE);

        if (phone == null || birthDate == null) {
            throw new IllegalArgumentException("가입에 필요한 정보(전화번호/생년월일)가 부족합니다.");
        }
        // 제공자가 전화번호를 안 줘서 앱에서 입력받은 경우 → SMS 인증을 거쳤는지 확인
        if (profile.phone() == null) {
            phoneVerificationService.ensureVerified(phone, Purpose.SIGNUP);
        }

        User user = User.builder()
                .email(profile.email())
                .password(null)                 // 소셜 전용 계정 → 비밀번호 없음
                .name(profile.name())
                .phone(phone)
                .birthDate(birthDate)
                .gender(gender)
                .address(null)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        saveTermsAgreements(user.getId());
        return user;
    }

    // 소셜계정 ↔ 회원 연동 레코드 저장 (이미 연동돼 있으면 건너뜀)
    private void linkSocialAccount(Provider provider, SocialProfile profile, Long userId) {
        boolean already = socialAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .isPresent();
        if (already) {
            return;
        }
        socialAccountRepository.save(SocialAccount.builder()
                .userId(userId)
                .provider(provider)
                .providerUserId(profile.providerUserId())
                .linkedAt(LocalDateTime.now())
                .build());
    }

    private User requireActive(User user) {
        if (user.isWithdrawn()) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }
        return user;
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = refreshTokenService.issue(user.getId());
        return new LoginResponse(user.getId(), accessToken, refreshToken, "Bearer");
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