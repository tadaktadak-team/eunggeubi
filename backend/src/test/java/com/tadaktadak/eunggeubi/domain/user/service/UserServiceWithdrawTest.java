package com.tadaktadak.eunggeubi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.auth.service.RefreshTokenService;
import com.tadaktadak.eunggeubi.domain.health.repository.HealthProfileRepository;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 탈퇴하면 등록한 보호자(이름·전화번호)와 동의 기록이 함께 지워지는지 확인한다.
class UserServiceWithdrawTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final GuardianConsentRepository consentRepository = mock(GuardianConsentRepository.class);
    private final HealthProfileRepository healthProfileRepository = mock(HealthProfileRepository.class);
    private UserService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, refreshTokenService, jwtProvider,
                guardianRepository, consentRepository, healthProfileRepository);
        user = mock(User.class);
        when(user.isWithdrawn()).thenReturn(false);
        when(user.getPassword()).thenReturn("hashed");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);
    }

    private Guardian guardian(long id) {
        Guardian g = mock(Guardian.class);
        when(g.getId()).thenReturn(id);
        return g;
    }

    @Test
    void 탈퇴하면_보호자와_동의_기록이_함께_지워진다() {
        List<Guardian> guardians = List.of(guardian(10L), guardian(11L));
        when(guardianRepository.findByUserId(1L)).thenReturn(guardians);

        service.withdraw(1L, "pw");

        verify(user).withdraw();
        verify(refreshTokenService).revokeAll(1L);
        // 보호자를 지우기 전에 그 보호자를 가리키는 동의 기록부터 지운다
        InOrder order = inOrder(consentRepository, guardianRepository);
        order.verify(consentRepository).deleteByGuardianIdIn(List.of(10L, 11L));
        order.verify(consentRepository).deleteByUserId(1L);
        order.verify(guardianRepository).deleteAll(guardians);
    }

    @Test
    void 탈퇴하면_건강_프로필도_함께_지워진다() {
        when(guardianRepository.findByUserId(1L)).thenReturn(List.of());

        service.withdraw(1L, "pw");

        verify(healthProfileRepository).deleteByUserId(1L);
    }

    @Test
    void 보호자를_따로_지운_뒤_남은_동의_기록도_회원번호로_지운다() {
        when(guardianRepository.findByUserId(1L)).thenReturn(List.of());

        service.withdraw(1L, "pw");

        verify(consentRepository).deleteByUserId(1L);
        verify(consentRepository, never()).deleteByGuardianIdIn(anyCollection());   // 빈 목록으로 IN 조회를 하지 않는다
    }

    @Test
    void 비밀번호가_틀리면_아무것도_지우지_않는다() {
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.withdraw(1L, "wrong")).isInstanceOf(IllegalArgumentException.class);

        verify(user, never()).withdraw();
        verify(healthProfileRepository, never()).deleteByUserId(any());
        verify(guardianRepository, never()).deleteAll(anyList());
        verify(consentRepository, never()).deleteByUserId(any());
        verify(consentRepository, never()).deleteByGuardianIdIn(anyCollection());
    }

    @Test
    void 이미_탈퇴한_계정이면_아무것도_지우지_않는다() {
        when(user.isWithdrawn()).thenReturn(true);

        assertThatThrownBy(() -> service.withdraw(1L, "pw")).isInstanceOf(IllegalArgumentException.class);

        verify(guardianRepository, never()).deleteAll(anyList());
        verify(consentRepository, never()).deleteByUserId(any());
    }
}
