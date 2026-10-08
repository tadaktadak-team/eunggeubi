package com.tadaktadak.eunggeubi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import com.tadaktadak.eunggeubi.domain.auth.service.PhoneVerificationService;
import com.tadaktadak.eunggeubi.domain.auth.service.RefreshTokenService;
import com.tadaktadak.eunggeubi.domain.health.repository.HealthProfileRepository;
import com.tadaktadak.eunggeubi.domain.user.dto.UpdateMyInfoRequest;
import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

// 회원 정보 수정에서 전화번호를 바꿀 때만 새 번호의 문자 인증을 요구하는지 확인한다.
class UserServiceUpdateInfoTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PhoneVerificationService phoneVerificationService = mock(PhoneVerificationService.class);
    private UserService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, mock(PasswordEncoder.class), mock(RefreshTokenService.class),
                mock(JwtProvider.class), mock(GuardianRepository.class), mock(GuardianConsentRepository.class),
                mock(HealthProfileRepository.class), phoneVerificationService);
        user = mock(User.class);
        when(user.getPhone()).thenReturn("010-1111-2222");   // 예전 데이터처럼 하이픈이 섞여 있어도 같은 번호로 본다
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    private UpdateMyInfoRequest request(String phone) {
        return new UpdateMyInfoRequest("홍길동", phone, LocalDate.of(1990, 1, 1), Gender.NONE, null);
    }

    @Test
    void 전화번호가_그대로면_인증을_요구하지_않는다() {
        service.updateMyInfo(1L, request("01011112222"));

        verify(phoneVerificationService, never()).consumeVerified(anyString(), any());
        verify(user).updateProfile("홍길동", "01011112222", LocalDate.of(1990, 1, 1), Gender.NONE, null);
    }

    @Test
    void 전화번호를_바꾸면_새_번호의_인증을_사용한다() {
        service.updateMyInfo(1L, request("01099998888"));

        verify(phoneVerificationService).consumeVerified("01099998888", Purpose.SIGNUP);
        verify(user).updateProfile("홍길동", "01099998888", LocalDate.of(1990, 1, 1), Gender.NONE, null);
    }

    @Test
    void 새_번호를_인증하지_않았으면_수정하지_않는다() {
        doThrow(new IllegalArgumentException("휴대폰 인증이 필요합니다."))
                .when(phoneVerificationService).consumeVerified("01099998888", Purpose.SIGNUP);

        assertThatThrownBy(() -> service.updateMyInfo(1L, request("01099998888")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(user, never()).updateProfile(any(), any(), any(), any(), any());
    }
}
