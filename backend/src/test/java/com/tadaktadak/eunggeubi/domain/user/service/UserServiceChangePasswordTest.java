package com.tadaktadak.eunggeubi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.auth.service.PhoneVerificationService;
import com.tadaktadak.eunggeubi.domain.auth.service.RefreshTokenService;
import com.tadaktadak.eunggeubi.domain.health.repository.HealthProfileRepository;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

// 비밀번호 변경에서도 이메일 아이디·전화번호 조각이 들어간 비밀번호를 막는지(가입·재설정과 같은 정책) 확인한다.
class UserServiceChangePasswordTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private UserService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, refreshTokenService, jwtProvider,
                mock(GuardianRepository.class), mock(GuardianConsentRepository.class), mock(HealthProfileRepository.class),
                mock(PhoneVerificationService.class));
        user = mock(User.class);
        when(user.getPassword()).thenReturn("hashed");
        when(user.getEmail()).thenReturn("hong.gildong@example.com");
        when(user.getPhone()).thenReturn("01012345678");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-pw", "hashed")).thenReturn(true);
        when(passwordEncoder.matches("Zq!9xKd2", "hashed")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(refreshTokenService.issue(1L)).thenReturn("new-refresh");
        when(jwtProvider.createAccessToken(1L)).thenReturn("new-access");
    }

    @Test
    void 정책을_지킨_새_비밀번호는_변경된다() {
        service.changePassword(1L, "current-pw", "Zq!9xKd2");

        verify(user).changePassword("encoded");
        verify(refreshTokenService).revokeAll(1L);
    }

    @Test
    void 이메일_아이디가_들어간_새_비밀번호는_거절한다() {
        assertThatThrownBy(() -> service.changePassword(1L, "current-pw", "Hong.Gildong#9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 아이디");
        verify(user, never()).changePassword(anyString());
    }

    @Test
    void 전화번호_조각이_들어간_새_비밀번호는_거절한다() {
        assertThatThrownBy(() -> service.changePassword(1L, "current-pw", "Zq!5678x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("전화번호");
        verify(user, never()).changePassword(anyString());
    }

    @Test
    void 현재_비밀번호가_틀리면_정책_검사보다_먼저_거절한다() {
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(1L, "wrong", "Zq!9xKd2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");
        assertThat(user).isNotNull();
    }
}
