package com.tadaktadak.eunggeubi.domain.auth.service;

import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import com.tadaktadak.eunggeubi.domain.auth.repository.RefreshTokenRepository;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserRepository userRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    // 아이디(이메일) 찾기: 이름 + 인증된 전화번호 → 마스킹된 이메일
    @Transactional(readOnly = true)
    public String findEmail(String name, String phone) {
        phoneVerificationService.ensureVerified(phone, Purpose.FIND_ID);
        User user = userRepository.findByNameAndPhone(name, phone)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
        return maskEmail(user.getEmail());
    }

    // 비밀번호 재설정: 이메일 + 인증된 전화번호 → 새 비밀번호 저장
    @Transactional
    public void resetPassword(String email, String phone, String newPassword) {
        phoneVerificationService.ensureVerified(phone, Purpose.FIND_PW);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));

        // 이메일 주인과 인증한 전화번호가 같은 사람인지 확인
        if (!user.getPhone().equals(phone)) {
            throw new IllegalArgumentException("일치하는 회원 정보가 없습니다.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));

        //다른 기기에 남아있는 세션을 전부 끊음(access 토큰의 경우 만료까지 최대 1시간 유효)
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())
                .forEach(token -> token.revoke(now));
    }

    // ab****@gmail.com 형태로 마스킹
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "****" + domain;
        }
        return local.substring(0, 2) + "****" + domain;
    }
}