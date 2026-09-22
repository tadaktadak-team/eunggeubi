package com.tadaktadak.eunggeubi.domain.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 소셜 로그인 진행 중 상태를 잠깐 저장한다.
 * - CSRF 방지용 state 값 검증
 * - 로그인 완료 후 어느 앱 주소(딥링크)로 돌려보낼지(appRedirect) 기억
 *
 * 개발/단일 인스턴스 기준 메모리 저장. (다중 인스턴스로 배포하면 Redis 등 공유 저장소로 교체)
 */
@Component
public class SocialAuthStateStore {

    private static final Duration TTL = Duration.ofMinutes(5);

    public record Pending(String appRedirect, Instant createdAt) {}

    private final Map<String, Pending> store = new ConcurrentHashMap<>();

    // state 와 함께, 로그인 후 돌아갈 앱 주소를 저장
    public void save(String state, String appRedirect) {
        store.put(state, new Pending(appRedirect, Instant.now()));
    }

    // state 를 소비(1회용)하고 저장해둔 appRedirect 를 반환. 없거나 만료면 null.
    public String consume(String state) {
        Pending p = store.remove(state);
        if (p == null) {
            return null;
        }
        if (p.createdAt().plus(TTL).isBefore(Instant.now())) {
            return null;
        }
        return p.appRedirect();
    }
}