package com.tadaktadak.eunggeubi.domain.auth.service;

import com.tadaktadak.eunggeubi.domain.auth.entity.PhoneVerification;
import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import com.tadaktadak.eunggeubi.domain.auth.entity.VerificationStatus;
import com.tadaktadak.eunggeubi.domain.auth.repository.PhoneVerificationRepository;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.common.MessageType;
import com.tadaktadak.eunggeubi.global.sms.SmsSender;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhoneVerificationService {
    private static final int VERIFIED_VALID_MINUTES = 10; // 인증 완료 후 실제로 쓸 수 있는 시간
    private static final int CODE_EXPIRY_MINUTES = 5;   // 인증번호 유효시간
    private static final int MAX_ATTEMPTS = 5;          // 최대 오입력 횟수

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final SmsSender smsSender;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();
    // 문자 발송 상한. RateLimitFilter 는 IP 기준이라 IP 를 바꿔가며 오면 못 막는다.
    @Value("${app.sms.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.sms.daily-limit-per-phone:5}")
    private int dailyLimitPerPhone;

    @Value("${app.sms.daily-limit-total:200}")
    private int dailyLimitTotal;

    // 인증번호 발급 + 발송
    @Transactional
    public void sendCode(String phone, Purpose purpose, String email) {
        // 비용·스팸 방어. 실제 발송 전에 먼저 막는다.
        enforceSendLimits(phone);
        // 비밀번호 찾기는 "그 이메일의 주인"에게만 코드를 보낸다 - 이메일+전화가 같은 회원인지 먼저
        // 확인하고, 아니면 코드 자체를 발송하지 않는다(임의 번호로 SMS를 유발하는 것도 막힌다).
        if (purpose == Purpose.FIND_PW) {
            verifyEmailOwnsPhone(email, phone);
        }

        String code = String.format("%06d", random.nextInt(1_000_000)); // 000000~999999
        LocalDateTime now = LocalDateTime.now();

        PhoneVerification verification = PhoneVerification.builder()
                .phone(phone)
                .code(code)
                .purpose(purpose)
                .status(VerificationStatus.PENDING)
                .attemptCount(0)
                .messageType(MessageType.SMS)
                .expiresAt(now.plusMinutes(CODE_EXPIRY_MINUTES))
                .createdAt(now)
                .build();
        phoneVerificationRepository.save(verification);

        smsSender.send(phone, "[응급이] 인증번호 [" + code + "]를 입력해주세요.");
    }
    // 발송 상한 검사. 이 메서드를 통과해야만 문자가 나간다.
    private void enforceSendLimits(String phone) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

        // 1) 같은 번호 재발송 쿨다운. 목적(SIGNUP/FIND_ID/FIND_PW)을 보지 않는다
        //    - 목적만 바꿔가며 쿨다운을 우회하는 걸 막기 위해서다.
        phoneVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .filter(last -> last.getCreatedAt().isAfter(now.minusSeconds(resendCooldownSeconds)))
                .ifPresent(last -> {
                    throw new IllegalArgumentException(
                            "인증번호는 " + resendCooldownSeconds + "초 후에 다시 요청할 수 있어요.");
                });

        // 2) 번호당 일일 상한 - 특정 번호로 문자 폭탄을 보내는 걸 막는다.
        if (phoneVerificationRepository.countByPhoneAndCreatedAtAfter(phone, todayStart) >= dailyLimitPerPhone) {
            throw new IllegalArgumentException("오늘 이 번호로 요청할 수 있는 횟수를 모두 사용했어요.");
        }

        // 3) 서비스 전체 일일 상한 = 문자 요금의 상한선.
        //    무작위 번호로 퍼붓는 공격은 1)2)로 막히지 않으므로 총량으로 잠근다.
        //    여기 걸리면 정상 사용자도 막히니, 공격 신호로 보고 로그를 남긴다.
        long totalToday = phoneVerificationRepository.countByCreatedAtAfter(todayStart);
        if (totalToday >= dailyLimitTotal) {
            log.error("[SMS] 전체 일일 발송 상한({}) 도달. 비정상 트래픽 여부를 확인할 것.", dailyLimitTotal);
            throw new IllegalArgumentException("인증 문자 발송이 일시적으로 제한되었어요. 잠시 후 다시 시도해주세요.");
        }
    }

    // 이메일 주인과 입력한 전화번호가 같은 회원인지 확인한다. 어느 쪽이 틀렸는지는 구분해서 알려주지
    // 않는다(이메일 존재 여부가 노출되면 계정 탐색에 악용될 수 있어 메시지를 하나로 통일 - resetPassword와 동일).
    private void verifyEmailOwnsPhone(String email, String phone) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("가입한 이메일을 입력해주세요.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
        if (!phone.equals(user.getPhone())) {
            throw new IllegalArgumentException("일치하는 회원 정보가 없습니다.");
        }
    }

    // 인증번호 검증
    @Transactional
    public void verifyCode(String phone, Purpose purpose, String code) {
        PhoneVerification verification = phoneVerificationRepository
                .findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 내역이 없습니다."));

        if (verification.getStatus() == VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("이미 인증이 완료된 번호입니다.");
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 요청해주세요.");
        }
        if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다. 다시 요청해주세요.");
        }
        if (!verification.getCode().equals(code)) {
            verification.increaseAttempt();
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        verification.markVerified(LocalDateTime.now());
    }

    // 가입/아이디찾기/비번재설정 직전에 호출한다.
    // "인증된 적 있는가"만 보면 몇 달 전 인증으로도 계속 통과하고, 한 번 인증으로
    // 비밀번호를 무한정 재설정할 수 있다. 그래서 (1) 최근에 인증했는지, (2) 아직 안 쓴
    // 인증인지까지 확인하고, 통과하면 그 자리에서 소비 처리한다.
    // readOnly 가 아니다 - 소비(쓰기)를 해야 하기 때문.
    @Transactional
    public void consumeVerified(String phone, Purpose purpose) {
        PhoneVerification verification = phoneVerificationRepository
                .findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> new IllegalArgumentException("휴대폰 인증이 필요합니다."));

        if (verification.getStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("휴대폰 인증이 완료되지 않았습니다.");
        }
        if (verification.getConsumedAt() != null) {
            throw new IllegalArgumentException("이미 사용된 인증입니다. 인증을 다시 받아주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (verification.getVerifiedAt() == null
                || verification.getVerifiedAt().isBefore(now.minusMinutes(VERIFIED_VALID_MINUTES))) {
            throw new IllegalArgumentException("인증 후 시간이 너무 지났어요. 인증을 다시 받아주세요.");
        }

        verification.consume(now);
    }
}