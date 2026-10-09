package com.tadaktadak.eunggeubi.domain.emergency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertRequest;
import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertResponse;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// 발송 횟수 제한에 걸리면 요청은 성공으로 응답하되(119 연결은 앱이 그대로 진행) 보호자 문자만 생략하는지 확인한다.
class EmergencyServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final EmergencyAlertNotifier notifier = mock(EmergencyAlertNotifier.class);
    private final EmergencyAlertThrottle throttle = mock(EmergencyAlertThrottle.class);
    private EmergencyService service;
    private final EmergencyAlertRequest request = new EmergencyAlertRequest(37.5, 127.0, "서울");

    @BeforeEach
    void setUp() {
        service = new EmergencyService(userRepository, guardianRepository, notifier, throttle);
        User user = mock(User.class);
        when(user.getName()).thenReturn("홍길동");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    private Guardian guardian(String phone) {
        Guardian g = mock(Guardian.class);
        when(g.getName()).thenReturn("엄마");
        when(g.getPhone()).thenReturn(phone);
        when(g.getRelationship()).thenReturn(Relationship.PARENT);
        return g;
    }

    @Test
    void 제한에_안_걸리면_보호자_모두에게_보낸다() {
        List<Guardian> guardians = List.of(guardian("01011112222"), guardian("01033334444"));
        when(guardianRepository.findByUserIdAndNotifyEnabledTrueAndVerifiedAtIsNotNull(1L)).thenReturn(guardians);
        when(throttle.tryAcquire(anyLong(), any())).thenReturn(EmergencyAlertThrottle.Result.ALLOWED);

        EmergencyAlertResponse res = service.sendAlert(1L, request);

        verify(notifier, times(2)).notifyGuardian(anyString(), anyString(), anyString());
        assertThat(res.skipReason()).isNull();
        assertThat(res.guardians()).extracting(EmergencyAlertResponse.GuardianResult::status).containsOnly("SENDING");
    }

    @Test
    void 일분_안에_다시_누르면_문자를_보내지_않고_이유를_알려준다() {
        List<Guardian> guardians = List.of(guardian("01011112222"));
        when(guardianRepository.findByUserIdAndNotifyEnabledTrueAndVerifiedAtIsNotNull(1L)).thenReturn(guardians);
        when(throttle.tryAcquire(anyLong(), any())).thenReturn(EmergencyAlertThrottle.Result.RECENTLY_SENT);

        EmergencyAlertResponse res = service.sendAlert(1L, request);

        verify(notifier, never()).notifyGuardian(anyString(), anyString(), anyString());
        assertThat(res.skipReason()).isEqualTo("RECENTLY_SENT");
        assertThat(res.guardians()).extracting(EmergencyAlertResponse.GuardianResult::status).containsOnly("SKIPPED");
    }

    @Test
    void 보낼_보호자가_없으면_횟수를_쓰지_않는다() {
        when(guardianRepository.findByUserIdAndNotifyEnabledTrueAndVerifiedAtIsNotNull(1L)).thenReturn(List.of());

        EmergencyAlertResponse res = service.sendAlert(1L, request);

        verify(throttle, never()).tryAcquire(anyLong(), any());
        assertThat(res.skipReason()).isNull();
    }
}
