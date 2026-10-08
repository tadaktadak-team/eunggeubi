package com.tadaktadak.eunggeubi.domain.auth.service;
import com.tadaktadak.eunggeubi.domain.auth.dto.ConsentPageInfo;
import com.tadaktadak.eunggeubi.domain.auth.dto.ConsentStatusResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianConsentResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianRequest;
import com.tadaktadak.eunggeubi.domain.user.entity.ConsentPurpose;
import com.tadaktadak.eunggeubi.domain.user.entity.ConsentStatus;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.GuardianConsent;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.common.MessageType;
import com.tadaktadak.eunggeubi.global.exception.ExternalApiException;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import com.tadaktadak.eunggeubi.global.sms.SmsSender;
import com.tadaktadak.eunggeubi.global.util.PhoneNumbers;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GuardianConsentService {

    private static final int CONSENT_VALID_DAYS = 3;
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_PER_GUARDIAN_PER_DAY = 3;
    private static final int MAX_PER_PHONE_PER_DAY = 3;
    private static final int MAX_PER_USER_PER_DAY = 10;
    private static final String DAILY_LIMIT_MESSAGE = "오늘 보낼 수 있는 동의 문자 횟수를 넘었어요. 내일 다시 시도해주세요.";

    private final GuardianRepository guardianRepository;
    private final GuardianConsentRepository guardianConsentRepository;
    private final UserRepository userRepository;
    private final SmsSender smsSender;
    private final JwtProvider jwtProvider;

    // 동의 링크의 서버 주소. 운영은 app.base-url로 고정하고(요청 헤더를 믿지 않는다),
    // 비어 있는 로컬 개발에서만 요청이 들어온 주소(예: 같은 와이파이의 PC IP)를 쓴다
    @Value("${app.base-url:}")
    private String configuredBaseUrl;

    @Transactional
    public GuardianConsentResponse requestConsent(GuardianRequest request) {
        // 1. 대상 회원 확인 (동의 대기 상태여야 함)
        User user = userRepository.findById(requireUserId(request.consentToken()))
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalArgumentException("보호자 동의가 필요한 상태가 아닙니다.");
        }
        if (PhoneNumbers.digitsOnly(user.getPhone()).equals(request.phone())) {
            throw new IllegalArgumentException("본인 번호는 보호자로 등록할 수 없어요.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 가입 응답의 동의 토큰은 3일간 쓸 수 있다. 상한이 없으면 같은 토큰으로 아무 번호에나 문자를 계속 보낼 수 있어서
        // (IP당 분당 5건 제한만으로는 부족하다) 마이페이지 등록과 같은 간격·일일 상한을 건다
        if (guardianConsentRepository.countByUserIdAndSentAtAfter(user.getId(), now.minus(RESEND_COOLDOWN)) > 0) {
            throw new IllegalArgumentException("잠시 후 다시 시도해주세요. 동의 문자는 1분에 한 번만 보낼 수 있어요.");
        }
        enforceDailyLimits(user.getId(), request.phone(), now);

        // 2. 보호자 저장
        Guardian guardian = guardianRepository.save(Guardian.builder()
                .userId(user.getId())
                .name(request.name())
                .phone(request.phone())
                .relationship(request.relationship())
                .notifyEnabled(true)
                .createdAt(now)
                .build());

        // 3. 동의 요청 생성 (랜덤 토큰 발급)
        String token = UUID.randomUUID().toString().replace("-", "");
        guardianConsentRepository.save(GuardianConsent.builder()
                .guardianId(guardian.getId())
                .userId(user.getId())
                .phone(request.phone())
                .purpose(ConsentPurpose.SIGNUP)
                .consentToken(token)
                .status(ConsentStatus.PENDING)
                .messageType(MessageType.SMS)
                .sentAt(now)
                .expiresAt(now.plusDays(CONSENT_VALID_DAYS))
                .build());

        // 4. 보호자에게 동의 링크 문자 발송 (개발 중엔 콘솔에 찍힘)
        String link = confirmLink(token);
        smsSender.send(request.phone(),
                "[응급이] 자녀의 보호자 동의 요청입니다. 아래 링크를 눌러 동의해주세요.\n" + link);

        return new GuardianConsentResponse(maskPhone(request.phone()));
    }

    private String confirmLink(String token) {
        String baseUrl = configuredBaseUrl.isBlank()
                ? ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                : configuredBaseUrl;
        return baseUrl + "/consent?t=" + token;
    }

    @Transactional
    public ConsentPurpose confirmConsent(String token) {
        // 1. 토큰으로 동의 요청 조회
        GuardianConsent consent = guardianConsentRepository.findByConsentToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 동의 링크입니다."));
        ConsentPurpose purpose = consent.getPurpose() != null ? consent.getPurpose() : ConsentPurpose.SIGNUP;

        // 2. 이미 동의됐으면 멱등 처리
        if (consent.getStatus() == ConsentStatus.CONFIRMED) {
            return purpose;
        }

        // 3. 만료 확인 (새 링크가 발급돼 무효화된 경우 포함)
        LocalDateTime now = LocalDateTime.now();
        if (consent.getStatus() == ConsentStatus.EXPIRED
                || (consent.getExpiresAt() != null && consent.getExpiresAt().isBefore(now))) {
            consent.expire();
            throw new IllegalArgumentException("만료된 동의 링크입니다. 앱에서 다시 요청해주세요.");
        }

        // 4. 동의 처리: 보호자 인증 + (미성년자 가입이면) 회원 활성화
        consent.confirm(now);
        Guardian guardian = guardianRepository.findById(consent.getGuardianId())
                .orElseThrow(() -> new IllegalArgumentException("보호자 정보를 찾을 수 없습니다."));
        guardian.markVerified(now);
        if (purpose == ConsentPurpose.SIGNUP) {
            User user = userRepository.findById(guardian.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
            user.activate();
        }
        return purpose;
    }

    // 이미 가입한 회원이 보호자를 등록·수정·재요청할 때: 보호자 본인의 수신 동의 문자를 보낸다
    @Transactional
    public void requestVerification(Guardian guardian) {
        User user = userRepository.findById(guardian.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(1);
        Long guardianId = guardian.getId();

        guardianConsentRepository.findTopByGuardianIdOrderBySentAtDesc(guardianId)
                .filter(c -> guardian.getPhone().equals(c.getPhone())
                        && c.getSentAt() != null && c.getSentAt().isAfter(now.minus(RESEND_COOLDOWN)))
                .ifPresent(c -> {
                    throw new IllegalArgumentException("잠시 후 다시 시도해주세요. 동의 문자는 1분에 한 번만 보낼 수 있어요.");
                });
        if (guardianConsentRepository.countByGuardianIdAndSentAtAfter(guardianId, since) >= MAX_PER_GUARDIAN_PER_DAY) {
            throw new IllegalArgumentException(DAILY_LIMIT_MESSAGE);
        }
        enforceDailyLimits(user.getId(), guardian.getPhone(), now);

        // 먼저 보낸 링크는 무효화하고 가장 최근 링크만 쓸 수 있게 한다
        guardianConsentRepository.findByGuardianIdAndStatus(guardianId, ConsentStatus.PENDING)
                .forEach(GuardianConsent::expire);

        String token = UUID.randomUUID().toString().replace("-", "");
        guardianConsentRepository.save(GuardianConsent.builder()
                .guardianId(guardianId)
                .userId(user.getId())
                .phone(guardian.getPhone())
                .purpose(ConsentPurpose.REGISTRATION)
                .consentToken(token)
                .status(ConsentStatus.PENDING)
                .messageType(MessageType.SMS)
                .sentAt(now)
                .expiresAt(now.plusDays(CONSENT_VALID_DAYS))
                .build());

        String link = confirmLink(token);
        String text = user.getName() + "님이 회원님을 응급 상황 알림을 받는 보호자로 등록했습니다.\n"
                + "동의하시면 아래 링크를 눌러 확인해 주세요. 동의하기 전에는 어떤 알림도 발송되지 않습니다.\n"
                + "모르는 분이라면 무시하셔도 됩니다. (링크는 " + CONSENT_VALID_DAYS + "일 후 만료)\n"
                + link;
        try {
            smsSender.send(guardian.getPhone(), "[응급이] 보호자 등록 동의 요청", text);
        } catch (Exception e) {
            throw new ExternalApiException("동의 문자를 보내지 못했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    // 같은 번호나 같은 회원이 하루에 받을 수 있는 동의 문자 수를 제한한다(문자 비용과 스팸 방지).
    // 가입 경로와 마이페이지 등록 경로가 똑같이 쓴다.
    private void enforceDailyLimits(Long userId, String phone, LocalDateTime now) {
        LocalDateTime since = now.minusDays(1);
        if (guardianConsentRepository.countByPhoneAndSentAtAfter(phone, since) >= MAX_PER_PHONE_PER_DAY
                || guardianConsentRepository.countByUserIdAndSentAtAfter(userId, since) >= MAX_PER_USER_PER_DAY) {
            throw new IllegalArgumentException(DAILY_LIMIT_MESSAGE);
        }
    }

    // 동의 확정 전에 보여줄 안내 화면용 정보. 상태를 바꾸지 않는다(GET 은 부작용이 없어야 한다).
    @Transactional(readOnly = true)
    public ConsentPageInfo loadConsentPage(String token) {
        GuardianConsent consent = guardianConsentRepository.findByConsentToken(token).orElse(null);
        if (consent == null) {
            return new ConsentPageInfo(ConsentPageInfo.State.INVALID, null, ConsentPurpose.SIGNUP);
        }
        ConsentPurpose purpose = consent.getPurpose() != null ? consent.getPurpose() : ConsentPurpose.SIGNUP;
        if (consent.getStatus() == ConsentStatus.CONFIRMED) {
            return new ConsentPageInfo(ConsentPageInfo.State.ALREADY_CONFIRMED, null, purpose);
        }
        // 새 링크가 발급돼 무효화된 링크(EXPIRED)도 만료로 안내한다
        if (consent.getStatus() == ConsentStatus.EXPIRED
                || (consent.getExpiresAt() != null && consent.getExpiresAt().isBefore(LocalDateTime.now()))) {
            return new ConsentPageInfo(ConsentPageInfo.State.EXPIRED, null, purpose);
        }
        return new ConsentPageInfo(ConsentPageInfo.State.READY, childNameOf(consent), purpose);
    }

    // 동의 화면에 "누구의 보호자 동의인지" 보여주기 위한 자녀 이름
    private String childNameOf(GuardianConsent consent) {
        Guardian guardian = guardianRepository.findById(consent.getGuardianId()).orElse(null);
        if (guardian == null) {
            return null;
        }
        return userRepository.findById(guardian.getUserId()).map(User::getName).orElse(null);
    }

    @Transactional(readOnly = true)
    public ConsentStatusResponse getStatus(String consentToken) {
        User user = userRepository.findById(requireUserId(consentToken))
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return new ConsentStatusResponse(user.getStatus() == UserStatus.ACTIVE);
    }

    // 가입 응답으로 받은 consentToken 에서 회원 id 를 꺼낸다.
    // 서명이 검증되므로 남의 userId 를 임의로 지정할 수 없다.
    private Long requireUserId(String consentToken) {
        if (!jwtProvider.isConsentToken(consentToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 요청입니다. 처음부터 다시 진행해주세요.");
        }
        return jwtProvider.getUserId(consentToken);
    }

    // 전화번호 뒤 4자리 가리기
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}