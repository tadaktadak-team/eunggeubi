package com.tadaktadak.eunggeubi.domain.auth.service;

import com.tadaktadak.eunggeubi.domain.auth.dto.LoginResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 소셜 로그인에 성공한 "기존 회원"의 토큰을 아주 잠깐 보관한다.
 *
 * 콜백에서 토큰을 리다이렉트 URL 쿼리에 실어 보내면
 *  - eunggeubi:// 는 소유권 검증이 없는 커스텀 스킴이라 다른 앱이 같은 스킴을 등록해 가로챌 수 있고
 *  - URL 은 OS 로그·브라우저 기록에 남는다.
 * 그래서 티켓만 넘기고, 앱이 POST /api/auth/social/exchange 로 본문에서 토큰을 받아가게 한다.
 *
 * 앱이 복귀하자마자 바로 교환하므로 TTL 은 짧게 잡는다.
 * (개발/단일 인스턴스 기준 메모리 저장. 다중 인스턴스면 Redis 등으로 교체)
 */
@Component
public class SocialLoginTicketStore {

    private static final Duration TTL = Duration.ofSeconds(60);

    private record Pending(LoginResponse tokens, Instant createdAt) {}

    private final Map<String, Pending> store = new ConcurrentHashMap<>();

    public void save(String ticket, LoginResponse tokens) {
        store.put(ticket, new Pending(tokens, Instant.now()));
    }

    // ticket 을 소비(1회용)하고 토큰을 반환. 없거나 만료면 null.
    public LoginResponse consume(String ticket) {
        if (ticket == null) {
            return null;
        }
        Pending p = store.remove(ticket);
        if (p == null || p.createdAt().plus(TTL).isBefore(Instant.now())) {
            return null;
        }
        return p.tokens();
    }
}