package com.tadaktadak.eunggeubi.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // 심평원(병원/약국) XML 응답 파싱용
    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }

    // XmlMapper가 ObjectMapper를 상속해서, 이 빈이 없으면 XmlMapper가 JSON 기본 매퍼로 잡힌다.
    // (응답이 JSON 대신 XML로 나가는 문제) 그래서 @Primary로 JSON 기본 매퍼를 명시한다.
    // 또한 Boot 빌더로 만들어야 JavaTimeModule이 포함되어 LocalDate/LocalDateTime을 처리할 수 있다.
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }
}