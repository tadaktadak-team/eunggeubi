package com.tadaktadak.eunggeubi.global.config;

import com.tadaktadak.eunggeubi.global.security.JwtAuthenticationEntryPoint;
import com.tadaktadak.eunggeubi.global.security.JwtAuthenticationFilter;
import com.tadaktadak.eunggeubi.global.security.JwtProvider;
import com.tadaktadak.eunggeubi.global.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // 앞단에 X-Forwarded-For를 덮어써주는 신뢰된 프록시(nginx/ALB 등)가 확실히 있을 때만 true로 켠다.
    // 기본 false: 이 헤더는 클라이언트가 임의로 채울 수 있어서, 잘못 켜면 레이트리밋이 그냥 우회된다.
    @Value("${app.rate-limit.trust-proxy:false}")
    private boolean trustProxyForwardedFor;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT 방식이라 세션/CSRF/기본로그인폼 다 끔
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // 경로별 접근 권한
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/hospitals/**",
                                "/api/pharmacies",
                                "/api/emergency-beds",
                                "/api/drugs/**"
                        ).permitAll()   // 인증/위치/약품 정보 전역 허용
                        .requestMatchers("/health", "/error").permitAll()        // 서버 상태체크
                        .requestMatchers("/api/ai-consultations/**").permitAll()  // AI 증상 상담: 비로그인도 이용 가능
                        .anyRequest().authenticated()                  // 나머지는 토큰 필수
                )

                // 시큐리티 기본 필터 앞에 우리 JWT 필터 끼워넣기
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class)
                // AI 상담은 permitAll이라 JWT 인증으로도 막을 수 없다 - 그 앞에서 IP 기준으로 먼저 거른다.
                .addFilterBefore(new RateLimitFilter(trustProxyForwardedFor), JwtAuthenticationFilter.class);

        return http.build();
    }

    // 비밀번호 암호화기 (BCrypt) - 회원가입/로그인에서 사용
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}