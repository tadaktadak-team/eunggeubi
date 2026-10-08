package com.tadaktadak.eunggeubi.domain.auth.service;
import com.tadaktadak.eunggeubi.domain.auth.dto.ConsentPageInfo;
import com.tadaktadak.eunggeubi.domain.auth.dto.ConsentStatusResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianConsentResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianConsentRequest;
import com.tadaktadak.eunggeubi.domain.user.entity.ConsentStatus;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.GuardianConsent;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.common.MessageType;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import com.tadaktadak.eunggeubi.global.sms.SmsSender;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuardianConsentService {

    private static final int CONSENT_VALID_DAYS = 3;

    private final GuardianRepository guardianRepository;
    private final GuardianConsentRepository guardianConsentRepository;
    private final UserRepository userRepository;
    private final SmsSender smsSender;
    private final JwtProvider jwtProvider;

    // 문자에 넣을 동의 링크의 서버 주소 (없으면 localhost 기본값)
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public GuardianConsentResponse requestConsent(GuardianConsentRequest request) {
        // 1. 대상 회원 확인 (동의 대기 상태여야 함)
        User user = userRepository.findById(requireUserId(request.consentToken()))
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalArgumentException("보호자 동의가 필요한 상태가 아닙니다.");
        }

        LocalDateTime now = LocalDateTime.now();

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
                .consentToken(token)
                .status(ConsentStatus.PENDING)
                .messageType(MessageType.SMS)
                .sentAt(now)
                .expiresAt(now.plusDays(CONSENT_VALID_DAYS))
                .build());

        // 4. 보호자에게 동의 링크 문자 발송 (개발 중엔 콘솔에 찍힘)
        String link = baseUrl + "/consent?t=" + token;
        smsSender.send(request.phone(),
                "[응급이] 자녀의 보호자 동의 요청입니다. 아래 링크를 눌러 동의해주세요.\n" + link);

        return new GuardianConsentResponse(maskPhone(request.phone()));
    }

    @Transactional
    public void confirmConsent(String token) {
        // 1. 토큰으로 동의 요청 조회
        GuardianConsent consent = guardianConsentRepository.findByConsentToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 동의 링크입니다."));

        // 2. 이미 동의됐으면 멱등 처리
        if (consent.getStatus() == ConsentStatus.CONFIRMED) {
            return;
        }

        // 3. 만료 확인
        LocalDateTime now = LocalDateTime.now();
        if (consent.getExpiresAt() != null && consent.getExpiresAt().isBefore(now)) {
            consent.expire();
            throw new IllegalArgumentException("만료된 동의 링크입니다. 다시 요청해주세요.");
        }

        // 4. 동의 처리 + 회원 활성화
        consent.confirm(now);
        Guardian guardian = guardianRepository.findById(consent.getGuardianId())
                .orElseThrow(() -> new IllegalArgumentException("보호자 정보를 찾을 수 없습니다."));
        User user = userRepository.findById(guardian.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        user.activate();
    }
    // 동의 확정 전에 보여줄 안내 화면용 정보. 상태를 바꾸지 않는다(GET 은 부작용이 없어야 한다).
    @Transactional(readOnly = true)
    public ConsentPageInfo loadConsentPage(String token) {
        GuardianConsent consent = guardianConsentRepository.findByConsentToken(token).orElse(null);
        if (consent == null) {
            return new ConsentPageInfo(ConsentPageInfo.State.INVALID, null);
        }
        if (consent.getStatus() == ConsentStatus.CONFIRMED) {
            return new ConsentPageInfo(ConsentPageInfo.State.ALREADY_CONFIRMED, null);
        }
        if (consent.getExpiresAt() != null && consent.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new ConsentPageInfo(ConsentPageInfo.State.EXPIRED, null);
        }
        return new ConsentPageInfo(ConsentPageInfo.State.READY, childNameOf(consent));
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