package com.tadaktadak.eunggeubi.domain.user.service;

import com.tadaktadak.eunggeubi.domain.auth.repository.RefreshTokenRepository;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.domain.user.dto.ProfileResponse;
import com.tadaktadak.eunggeubi.domain.user.dto.UpdateProfileRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void withdraw(Long userId, String password) {
        // 1. 회원 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 2. 이미 탈퇴한 계정이면 거부
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new IllegalArgumentException("이미 탈퇴한 계정입니다.");
        }

        // 3. 비밀번호 확인 (소셜 전용 계정은 password가 null이라 자동 실패)
        if (user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        // 4. 계정 탈퇴 처리 (상태 → WITHDRAWN)
        user.withdraw();

        // 5. 서버에 저장된 refresh 토큰 전부 폐기 (재로그인 불가)
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.revoke(now));
    }
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return ProfileResponse.from(user);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        user.updateProfile(request.name(), request.phone(), request.birthDate(),
                request.gender(), request.address());
        return ProfileResponse.from(user);   // 변경 감지로 자동 UPDATE
    }
}