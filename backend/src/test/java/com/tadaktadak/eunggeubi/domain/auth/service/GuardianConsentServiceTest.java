package com.tadaktadak.eunggeubi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianRequest;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import com.tadaktadak.eunggeubi.global.sms.SmsSender;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// 동의 문자의 간격과 일일 상한(문자 비용·스팸 방지)을 가입 경로와 마이페이지 등록 경로 모두 지키는지 확인한다.
class GuardianConsentServiceTest {

    private static final String GUARDIAN_PHONE = "01099998888";

    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final GuardianConsentRepository consentRepository = mock(GuardianConsentRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SmsSender smsSender = mock(SmsSender.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private GuardianConsentService service;

    @BeforeEach
    void setUp() {
        service = new GuardianConsentService(guardianRepository, consentRepository, userRepository, smsSender, jwtProvider);
        ReflectionTestUtils.setField(service, "configuredBaseUrl", "https://example.test");

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("아이");
        when(user.getPhone()).thenReturn("01011112222");
        when(user.getStatus()).thenReturn(UserStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtProvider.isConsentToken(anyString())).thenReturn(true);
        when(jwtProvider.getUserId(anyString())).thenReturn(1L);
        when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private GuardianRequest request() {
        return new GuardianRequest("consent-token", "보호자", GUARDIAN_PHONE, Relationship.PARENT);
    }

    // ---- 가입 경로 ----

    @Test
    void 가입_동의_요청은_상한_안이면_문자를_한_번_보낸다() {
        service.requestConsent(request());

        verify(smsSender).send(eq(GUARDIAN_PHONE), contains("https://example.test/consent?t="));
    }

    @Test
    void 가입_동의_요청은_1분_안에_다시_보내면_거절한다() {
        when(consentRepository.countByUserIdAndSentAtAfter(eq(1L), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.requestConsent(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1분에 한 번");
        verify(smsSender, never()).send(anyString(), anyString());
        verify(guardianRepository, never()).save(any());
    }

    @Test
    void 가입_동의_요청은_같은_번호로_하루_3건이_넘으면_거절한다() {
        when(consentRepository.countByPhoneAndSentAtAfter(eq(GUARDIAN_PHONE), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.requestConsent(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("오늘 보낼 수 있는");
        verify(smsSender, never()).send(anyString(), anyString());
        verify(guardianRepository, never()).save(any());
    }

    @Test
    void 가입_동의_요청은_한_회원이_하루_10건이_넘으면_거절한다() {
        // 첫 호출은 1분 간격 확인, 두 번째 호출이 하루 상한 확인이다
        when(consentRepository.countByUserIdAndSentAtAfter(eq(1L), any())).thenReturn(0L, 10L);

        assertThatThrownBy(() -> service.requestConsent(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("오늘 보낼 수 있는");
        verify(smsSender, never()).send(anyString(), anyString());
    }

    // ---- 마이페이지 등록 경로(기존 상한이 그대로인지) ----

    @Test
    void 마이페이지_등록_동의는_보호자별_하루_3건이_넘으면_거절한다() {
        Guardian guardian = mock(Guardian.class);
        when(guardian.getId()).thenReturn(7L);
        when(guardian.getUserId()).thenReturn(1L);
        when(guardian.getPhone()).thenReturn(GUARDIAN_PHONE);
        when(consentRepository.countByGuardianIdAndSentAtAfter(eq(7L), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.requestVerification(guardian))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("오늘 보낼 수 있는");
        verify(smsSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 마이페이지_등록_동의는_같은_번호로_하루_3건이_넘으면_거절한다() {
        Guardian guardian = mock(Guardian.class);
        when(guardian.getId()).thenReturn(7L);
        when(guardian.getUserId()).thenReturn(1L);
        when(guardian.getPhone()).thenReturn(GUARDIAN_PHONE);
        when(consentRepository.countByPhoneAndSentAtAfter(eq(GUARDIAN_PHONE), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.requestVerification(guardian))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("오늘 보낼 수 있는");
        verify(smsSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 마이페이지_등록_동의는_상한_안이면_문자를_보낸다() {
        Guardian guardian = mock(Guardian.class);
        when(guardian.getId()).thenReturn(7L);
        when(guardian.getUserId()).thenReturn(1L);
        when(guardian.getPhone()).thenReturn(GUARDIAN_PHONE);

        service.requestVerification(guardian);

        verify(smsSender).send(eq(GUARDIAN_PHONE), anyString(), contains("https://example.test/consent?t="));
        assertThat(GUARDIAN_PHONE).hasSize(11);
    }
}
