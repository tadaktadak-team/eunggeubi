package com.tadaktadak.eunggeubi.domain.user.service;

import com.tadaktadak.eunggeubi.domain.auth.service.RefreshTokenService;
import com.tadaktadak.eunggeubi.domain.user.dto.ChangePasswordResponse;
import com.tadaktadak.eunggeubi.domain.user.dto.MyInfoResponse;
import com.tadaktadak.eunggeubi.domain.user.dto.UpdateMyInfoRequest;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;
    private final GuardianRepository guardianRepository;
    private final GuardianConsentRepository guardianConsentRepository;

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        return MyInfoResponse.from(user);
    }

    @Transactional
    public ChangePasswordResponse changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        //카카오/네이버로만 가입한 회원은 비밀번호 자체가 없어서 변경 대상 아님
        if (user.getPassword() == null) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }

        //현재 비밀번호로 본인 재확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호와 다른 비밀번호를 입력해주세요.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));

        //다른 기기에 남아있는 세션을 전부 끊음(access 토큰의 경우 만료까지 최대 1시간 유효), 단 현재 기기는 유지(폐기->발급)
        refreshTokenService.revokeAll(userId);
        String newRefreshToken = refreshTokenService.issue(userId);
        String newAccessToken = jwtProvider.createAccessToken(userId);

        return new ChangePasswordResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public MyInfoResponse updateMyInfo(Long userId, UpdateMyInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        user.updateProfile(request.name().trim(), request.phone().trim(),
                request.birthDate(), request.gender(),
                request.address() == null ? null : request.address().trim());

        return MyInfoResponse.from(user);
    }

    @Transactional
    public void withdraw(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        if (user.isWithdrawn()) {
            throw new IllegalArgumentException("이미 탈퇴한 계정입니다.");
        }

        //소셜 전용 계정은 확인할 비밀번호가 없다
        if (user.getPassword() != null) {
            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        }

        user.withdraw();
        refreshTokenService.revokeAll(userId);

        // 등록한 보호자(이름·전화번호)와 동의 기록을 함께 지운다. 탈퇴 화면에서도, 보호자가 받는 동의 화면에서도
        // "탈퇴 시까지만 보관"한다고 안내하고 있다.
        List<Guardian> guardians = guardianRepository.findByUserId(userId);
        if (!guardians.isEmpty()) {   // 빈 목록으로 IN 조회를 하지 않는다
            guardianConsentRepository.deleteByGuardianIdIn(guardians.stream().map(Guardian::getId).toList());
        }
        guardianConsentRepository.deleteByUserId(userId);
        guardianRepository.deleteAll(guardians);
    }
}
