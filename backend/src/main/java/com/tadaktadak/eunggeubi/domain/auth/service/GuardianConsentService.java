package com.tadaktadak.eunggeubi.domain.auth.service;

import com.tadaktadak.eunggeubi.domain.auth.dto.ConsentStatusResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianConsentResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianRequest;
import com.tadaktadak.eunggeubi.domain.user.entity.ConsentStatus;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.GuardianConsent;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.common.MessageType;
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

    // 문자에 넣을 동의 링크의 서버 주소 (없으면 localhost 기본값)
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public GuardianConsentResponse requestConsent(GuardianRequest request) {
        // 1. 대상 회원 확인 (동의 대기 상태여야 함)
        User user = userRepository.findById(request.userId())
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
        String link = baseUrl + "/api/auth/guardian/confirm?token=" + token;
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

    @Transactional(readOnly = true)
    public ConsentStatusResponse getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return new ConsentStatusResponse(user.getStatus() == UserStatus.ACTIVE);
    }

    // 전화번호 뒤 4자리 가리기
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}