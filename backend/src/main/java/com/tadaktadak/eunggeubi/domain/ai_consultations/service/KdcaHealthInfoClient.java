package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.KdcaHealthInfoResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// KDCA 건강정보 API 호출 담당. 네트워크 호출(fetchRawXml)과 XML 파싱(parse)을 분리해서,
// 파싱 로직은 토큰/네트워크 없이 fixture XML로 단위 테스트할 수 있게 한다.
@Component
public class KdcaHealthInfoClient {

    private final RestClient restClient;
    private final XmlMapper xmlMapper;
    private final String baseUrl;
    private final String token;

    // RestClient.create() 대신 Spring이 자동구성한 RestClient.Builder를 받아서 쓴다.
    // 이래야 application.yml의 spring.http.client.connect-timeout/read-timeout(전역 타임아웃)이
    // 적용된다 - 직접 RestClient.create()로 만들면 그 전역 설정을 안 타서 응답이 없을 때
    // 무한정 멈추는 문제가 있었다.
    public KdcaHealthInfoClient(RestClient.Builder restClientBuilder,
                                 @Value("${kdca.health-info.base-url}") String baseUrl,
                                 @Value("${kdca.health-info.token}") String token) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.restClient = restClientBuilder.build();
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public KdcaHealthInfoResponse fetchDetail(String cntntsSn) {
        String rawXml = fetchRawXml(cntntsSn);
        return parse(rawXml);
    }

    // KDCA 응답은 Content-Type이 "text/html; charset=UTF-8"로 오는데, Spring의 기본
    // String 컨버터가 이 조합을 오인해서 한글이 깨지는 문제가 있었다(직접 curl로 받은 원본
    // 바이트는 정상 UTF-8인 것 확인함). 그래서 자동 판단에 맡기지 않고 byte[]로 받아
    // UTF-8로 직접 디코딩한다.
    String fetchRawXml(String cntntsSn) {
        byte[] bytes = restClient.get()
                .uri(baseUrl + "?TOKEN={token}&cntntsSn={id}", token, cntntsSn)
                .retrieve()
                .body(byte[].class);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    KdcaHealthInfoResponse parse(String xml) {
        try {
            return xmlMapper.readValue(xml, KdcaHealthInfoResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("KDCA 응답 XML 파싱 실패: " + e.getMessage(), e);
        }
    }
}
