package com.tadaktadak.eunggeubi.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.filter.OncePerRequestFilter;

// AI 상담 엔드포인트(/api/ai-consultations/**)는 permitAll이라 IP당 요청 수를 여기서 막지 않으면
// 익명이 무제한으로 LLM 호출을 태울 수 있다(비용 폭탄). IP당 분당 고정 횟수만 허용하는 최소 구현.
//
// ponytail: 인스턴스 로컬 고정 윈도우 카운터라 서버가 여러 대로 스케일아웃되면 IP당 실질 허용량이
// 인스턴스 수만큼 늘어난다. 여러 대로 늘리게 되면 Redis 등 공유 저장소 기반 카운터로 교체할 것.
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LIMITED_PATH_PREFIX = "/api/ai-consultations";
    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final long WINDOW_MS = 60_000;

    private final boolean trustForwardedFor;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    // 매 요청마다 청소하면 요청마다 맵 전체를 훑게 되니, 한 윈도우당 한 번만 청소한다(느슨한 상한).
    private final AtomicLong lastCleanupMs = new AtomicLong(System.currentTimeMillis());

    // trustForwardedFor=false(기본값)면 X-Forwarded-For를 무시하고 항상 remoteAddr만 쓴다.
    // 이 헤더는 클라이언트가 마음대로 채울 수 있어서, 앞단에 그걸 덮어써주는 신뢰된 프록시가
    // 확실할 때만(app.rate-limit.trust-proxy=true) 켜야 한다 - 안 그러면 값 하나만 바꿔가며 요청해도
    // 이 필터를 그대로 우회한다.
    public RateLimitFilter(boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(LIMITED_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        cleanupIfDue(now);

        int countAfterThisRequest = windows.compute(clientIp(request), (key, window) ->
                (window == null || now - window.windowStartMs >= WINDOW_MS)
                        ? new Window(now, 1)
                        : window.increment()
        ).count;

        if (countAfterThisRequest > MAX_REQUESTS_PER_WINDOW) {
            response.setStatus(429); // Too Many Requests (jakarta.servlet엔 상수가 없음)
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // 만료된(윈도우 지난) 카운터를 정리한다 - 안 하면 맵이 계속 자라기만 한다(distinct IP 수만큼 누적).
    private void cleanupIfDue(long now) {
        long last = lastCleanupMs.get();
        if (now - last < WINDOW_MS) {
            return;
        }
        if (lastCleanupMs.compareAndSet(last, now)) { // 여러 스레드가 동시에 청소하지 않도록
            windows.entrySet().removeIf(e -> now - e.getValue().windowStartMs >= WINDOW_MS);
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long windowStartMs;
        private int count;

        private Window(long windowStartMs, int count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }

        private Window increment() {
            count++;
            return this;
        }
    }
}
