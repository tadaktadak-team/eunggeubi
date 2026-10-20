package com.tadaktadak.eunggeubi.domain.emergency.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tadaktadak.eunggeubi.domain.emergency.service.EmergencyAlertThrottle.Result;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

// 긴급 문자는 1분 안 재발동과 하루 10회 초과일 때만 생략하는지 확인한다.
class EmergencyAlertThrottleTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 8, 12, 0);
    private final EmergencyAlertThrottle throttle = new EmergencyAlertThrottle();

    @Test
    void 처음_발동은_보낸다() {
        assertThat(throttle.tryAcquire(1L, T0)).isEqualTo(Result.ALLOWED);
    }

    @Test
    void 일분_안에_다시_누르면_생략하고_일분이_지나면_보낸다() {
        throttle.tryAcquire(1L, T0);

        assertThat(throttle.tryAcquire(1L, T0.plusSeconds(59))).isEqualTo(Result.RECENTLY_SENT);
        assertThat(throttle.tryAcquire(1L, T0.plusSeconds(61))).isEqualTo(Result.ALLOWED);
    }

    @Test
    void 다른_회원의_발동은_서로_영향이_없다() {
        throttle.tryAcquire(1L, T0);

        assertThat(throttle.tryAcquire(2L, T0)).isEqualTo(Result.ALLOWED);
    }

    @Test
    void 하루_열_번을_넘으면_생략하고_하루가_지나면_다시_보낸다() {
        for (int i = 0; i < EmergencyAlertThrottle.MAX_PER_DAY; i++) {
            assertThat(throttle.tryAcquire(1L, T0.plusMinutes(2L * i))).isEqualTo(Result.ALLOWED);
        }

        assertThat(throttle.tryAcquire(1L, T0.plusHours(5))).isEqualTo(Result.DAILY_LIMIT);
        // 첫 발동에서 하루가 지나면 그 한 건이 빠져 다시 보낼 수 있다
        assertThat(throttle.tryAcquire(1L, T0.plusDays(1).plusMinutes(1))).isEqualTo(Result.ALLOWED);
    }

    @Test
    void 생략된_발동은_횟수에_세지_않는다() {
        throttle.tryAcquire(1L, T0);
        throttle.tryAcquire(1L, T0.plusSeconds(10));   // 생략
        throttle.tryAcquire(1L, T0.plusSeconds(20));   // 생략

        // 마지막으로 실제 보낸 시각(T0) 기준 1분이 지났으므로 보낸다
        assertThat(throttle.tryAcquire(1L, T0.plusSeconds(61))).isEqualTo(Result.ALLOWED);
    }
}
