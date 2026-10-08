package com.tadaktadak.eunggeubi.domain.auth.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 소셜 로그인 후 되돌아갈 앱 주소(appRedirect)가 우리 앱의 것인지 검증한다.
 *
 * 이 검증이 없으면 공격자가
 *     /api/auth/social/kakao/authorize?appRedirect=https://evil.com
 * 으로 유도해서, 콜백 때 발급되는 access/refresh 토큰을 자기 서버로 받아갈 수 있다
 * (오픈 리다이렉트 → 계정 탈취). 로그인 화면이 진짜 카카오라서 피해자가 알아채기 어렵다.
 *
 * 허용 목록은 환경마다 다르다. expo-linking 의 createURL() 이 내는 값이
 * 프로덕션 빌드(eunggeubi://), Expo Go(exp://), 웹 프리뷰(http://localhost:)로 갈리기 때문이다.
 * 기본값은 운영 기준(앱 스킴만)이고, local 프로필에서만 개발용 주소를 추가로 연다.
 */
@Slf4j
@Component
public class AppRedirectValidator {

    private final List<String> allowedPrefixes;

    // 쉼표로 구분된 문자열이 List<String> 으로 변환되어 주입된다(YAML 리스트가 아님에 주의).
    public AppRedirectValidator(
            @Value("${app.allowed-app-redirects}") List<String> allowedPrefixes) {
        this.allowedPrefixes = allowedPrefixes;
    }

    public void validate(String appRedirect) {
        if (appRedirect == null || appRedirect.isBlank()) {
            throw new IllegalArgumentException("잘못된 로그인 요청입니다.");
        }
        boolean allowed = allowedPrefixes.stream().anyMatch(appRedirect::startsWith);
        if (!allowed) {
            // 차단된 값을 남긴다 - 공격 탐지용이자, 새 실행환경에서 실제로 어떤 주소가
            // 오는지 확인하는 용도이기도 하다(Expo Go 등에서 막히면 이 로그를 보면 된다).
            log.warn("[AUTH] 허용되지 않은 appRedirect 차단: {}", appRedirect);
            throw new IllegalArgumentException("잘못된 로그인 요청입니다.");
        }
    }
}