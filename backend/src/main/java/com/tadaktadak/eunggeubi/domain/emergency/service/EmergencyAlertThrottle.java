package com.tadaktadak.eunggeubi.domain.emergency.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// 긴급 문자 발송 횟수 제한. 응급 기능이라 요청을 거절하지 않고(119 연결은 그대로) 보호자 문자만 생략한다.
// 기록은 메모리에만 두어 서버를 다시 띄우면 비워진다 - 응급 상황에서는 덜 막는 쪽이 낫다.
@Component
public class EmergencyAlertThrottle {

    static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(1);   // 이 안에 다시 누르면 같은 상황으로 본다
    static final Duration DAILY_WINDOW = Duration.ofDays(1);
    static final int MAX_PER_DAY = 10;

    public enum Result { ALLOWED, RECENTLY_SENT, DAILY_LIMIT }

    private final Map<Long, Deque<LocalDateTime>> sentAtByUser = new ConcurrentHashMap<>();

    // 보내도 되면 발송 시각을 기록하고 ALLOWED 를 돌려준다. 같은 회원의 동시 요청도 한 번만 통과한다
    public Result tryAcquire(Long userId, LocalDateTime now) {
        Result[] result = new Result[1];
        sentAtByUser.compute(userId, (id, history) -> {
            Deque<LocalDateTime> sentAts = history == null ? new ArrayDeque<>() : history;
            while (!sentAts.isEmpty() && !sentAts.peekFirst().isAfter(now.minus(DAILY_WINDOW))) {
                sentAts.pollFirst();
            }
            if (!sentAts.isEmpty() && sentAts.peekLast().isAfter(now.minus(DUPLICATE_WINDOW))) {
                result[0] = Result.RECENTLY_SENT;
            } else if (sentAts.size() >= MAX_PER_DAY) {
                result[0] = Result.DAILY_LIMIT;
            } else {
                sentAts.addLast(now);
                result[0] = Result.ALLOWED;
            }
            return sentAts.isEmpty() ? null : sentAts;
        });
        return result[0];
    }
}
