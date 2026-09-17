package com.tadaktadak.eunggeubi.domain.auth.service;

import com.tadaktadak.eunggeubi.domain.auth.dto.SocialProfile;
import com.tadaktadak.eunggeubi.domain.auth.entity.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 신규 소셜 가입 대기자를 잠깐 저장한다.
 * /callback 에서 "완전 신규"로 판별되면, 회원을 만들지 않고 네이버 프로필을 ticket 으로 보관.
 * 앱이 약관 동의 후 /complete 를 부를 때 ticket 으로 다시 꺼내 회원을 생성한다.
 * (개발/단일 인스턴스 기준 메모리 저장. 다중 인스턴스면 Redis 등으로 교체)
 */
@Component
public class SocialSignupTicketStore {

    private static final Duration TTL = Duration.ofMinutes(10); // 약관 읽고 동의할 시간

    public record Pending(Provider provider, SocialProfile profile, Instant createdAt) {}

    private final Map<String, Pending> store = new ConcurrentHashMap<>();

    public void save(String ticket, Provider provider, SocialProfile profile) {
        store.put(ticket, new Pending(provider, profile, Instant.now()));
    }

    // ticket 을 소비(1회용)하고 담아둔 값을 반환. 없거나 만료면 null.
    public Pending consume(String ticket) {
        Pending p = store.remove(ticket);
        if (p == null) {
            return null;
        }
        if (p.createdAt().plus(TTL).isBefore(Instant.now())) {
            return null;
        }
        return p;
    }
}