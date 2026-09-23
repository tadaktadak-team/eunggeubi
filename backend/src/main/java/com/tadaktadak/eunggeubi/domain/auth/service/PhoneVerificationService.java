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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final int CODE_EXPIRY_MINUTES = 5;   // 인증번호 유효시간
    private static final int MAX_ATTEMPTS = 5;          // 최대 오입력 횟수

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final SmsSender smsSender;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    // 인증번호 발급 + 발송
    @Transactional
    public void sendCode(String phone, Purpose purpose, String email) {
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

    // 가입/비번찾기 전, 해당 번호가 인증 완료 상태인지 확인 (아니면 예외)
    @Transactional(readOnly = true)
    public void ensureVerified(String phone, Purpose purpose) {
        PhoneVerification verification = phoneVerificationRepository
                .findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> new IllegalArgumentException("휴대폰 인증이 필요합니다."));

        if (verification.getStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("휴대폰 인증이 완료되지 않았습니다.");
        }
    }
}