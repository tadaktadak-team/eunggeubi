package com.tadaktadak.eunggeubi.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    // 쿠키가 아니라 Authorization 헤더(JWT)로 인증하기 때문에 오리진을 굳이 제한하지 않아도
    // CSRF/세션 탈취 같은 쿠키 기반 위험은 없다. 로컬 개발 중에는 react-native-web 프리뷰가
    // 매번 다른 LAN IP:포트(예: http://172.21.8.247:8081)에서 뜨므로 전부 허용한다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
