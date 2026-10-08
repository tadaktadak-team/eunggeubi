package com.tadaktadak.eunggeubi.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.filter.OncePerRequestFilter;

// permitAll 로 열려 있는 엔드포인트들을 IP당 요청 수로 제한한다. 막지 않으면 익명이
// 무제한으로 LLM 호출(비용)·SMS 발송(비용)·비밀번호 대입을 할 수 있다.
//
// 한계 1: IP 단위라 NAT 뒤 여러 명이 할당량을 나눠 쓰고, 반대로 IP를 바꿔가며 오면 총량이 늘어난다.
//         돈이 나가는 SMS 는 번호 단위 쿨다운/일일 상한을 PhoneVerificationService 에서 한 번 더 건다.
// 한계 2: 인스턴스 로컬 고정 윈도우 카운터라 서버가 여러 대로 스케일아웃되면 IP당 실질 허용량이
//         인스턴스 수만큼 늘어난다. 여러 대로 늘리면 Redis 등 공유 저장소 기반 카운터로 교체할 것.
public class RateLimitFilter extends OncePerRequestFilter {

    // 경로별 "IP당 분당 허용 횟수". 접두사가 겹치면 더 긴 쪽이 우선한다.
    private record Rule(String pathPrefix, int maxPerMinute) {}

    private static final List<Rule> RULES = List.of(
            new Rule("/api/ai-consultations",      10),  // LLM 호출 비용
            new Rule("/api/auth/phone/send",        5),  // SMS 건당 과금
            new Rule("/api/auth/guardian/request",  5),  // SMS 건당 과금
            new Rule("/api/auth/login",            20),  // 비밀번호 대입 (NAT 공유 고려해 넉넉히)
            new Rule("/api/auth/find-email",       10),  // 가입자 탐색
            new Rule("/api/auth/reset-password",   10),
            new Rule("/api/auth/social/exchange",  20)
    );

    // 비회원 guestCode 발급 제한이 같은 IP 기준을 쓰도록 request 에 실어 보낸다(AiConsultationController).
    public static final String CLIENT_IP_ATTR = "clientIp";
    private static final long WINDOW_MS = 60_000;

    // 요청 경로에 적용할 규칙. 없으면 null = 이 필터가 관여하지 않는다.
    private static Rule ruleFor(String uri) {
        Rule best = null;
        for (Rule rule : RULES) {
            if (uri.startsWith(rule.pathPrefix())
                    && (best == null || rule.pathPrefix().length() > best.pathPrefix().length())) {
                best = rule;
            }
        }
        return best;
    }

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
        return ruleFor(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        Rule rule = ruleFor(request.getRequestURI());
        long now = System.currentTimeMillis();
        cleanupIfDue(now);

        String ip = clientIp(request);
        // 비회원 guestCode 발급 제한(AiConsultationController)도 같은 IP 기준을 쓰도록 넘겨준다.
        request.setAttribute(CLIENT_IP_ATTR, ip);

        // 엔드포인트마다 버킷을 따로 쓴다. 로그인 시도가 AI 상담 할당량을 깎으면 안 된다.
        String key = rule.pathPrefix() + "|" + ip;
        int countAfterThisRequest = windows.compute(key, (k, window) ->

                (window == null || now - window.windowStartMs >= WINDOW_MS)
                        ? new Window(now, 1)
                        : window.increment()
        ).count;

        if (countAfterThisRequest > rule.maxPerMinute()) {
            response.setStatus(429); // Too Many Requests (jakarta.servlet엔 상수가 없음)
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", "60"); // 클라이언트가 재시도 시점을 알 수 있게
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
