package com.tadaktadak.eunggeubi.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5)) // 연결 시도 타임아웃 (5초)
                .readTimeout(Duration.ofSeconds(5))    // 응답 대기 타임아웃 (5초)
                .build();
    }
}