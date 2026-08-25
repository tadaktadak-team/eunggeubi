package com.tadaktadak.eunggeubi.domain.drug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.global.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openapi.e-drug.url}")
    private String apiUrl;

    @Value("${openapi.e-drug.service-key}")
    private String serviceKey;

    /**
     * 1. 약품명 키워드 검색 API 연동
     */
    public List<DrugInfoResponse> searchDrugsByName(String keyword) {
        List<DrugInfoResponse> resultList = new ArrayList<>();

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());

            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("itemName", encodedKeyword)
                    .queryParam("type", "json")
                    .queryParam("numOfRows", 10)
                    .build(true)
                    .toUri();

            String responseString = restTemplate.getForObject(uri, String.class);
            JsonNode rootNode = objectMapper.readTree(responseString);
            JsonNode itemsNode = rootNode.path("body").path("items");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    resultList.add(mapToDrugInfoResponse(item));
                }
            }
        } catch (Exception e) {
            log.error("e약은요 Open API 검색 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("e약은요 API 연동 중 오류가 발생했습니다.", e);
        }

        return resultList;
    }

    /**
     * 2. 약품 상세 조회 API 연동 (itemSeq/약품명 기반 조회)
     */
    public DrugInfoResponse getDrugDetail(String itemSeq) {
        String responseString;

        // 1) API 통신 처리 (itemName으로 검색하여 1건을 조회)
        try {
            String encodedItemSeq = URLEncoder.encode(itemSeq, StandardCharsets.UTF_8.toString());

            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("itemName", encodedItemSeq) 
                    .queryParam("type", "json")
                    .queryParam("numOfRows", 1)
                    .build(true)
                    .toUri();

            responseString = restTemplate.getForObject(uri, String.class);
            log.info("e약은요 API 상세조회 응답: {}", responseString);

        } catch (Exception e) {
            log.error("e약은요 Open API 상세조회 통신 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("e약은요 API 통신에 실패했습니다.", e);
        }

        // 2) 데이터 파싱 처리
        try {
            JsonNode rootNode = objectMapper.readTree(responseString);
            JsonNode itemsNode = rootNode.path("body").path("items");

            if (itemsNode.isArray() && !itemsNode.isEmpty()) {
                return mapToDrugInfoResponse(itemsNode.get(0));
            }
        } catch (Exception e) {
            log.error("e약은요 Open API 응답 파싱 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("e약은요 API 응답 파싱 중 오류가 발생했습니다.", e);
        }

        // 3) 통신 성공했으나 검색 결과가 없는 경우 -> 400 Bad Request
        throw new IllegalArgumentException("해당 약물 정보를 찾을 수 없습니다: " + itemSeq);
    }

    private DrugInfoResponse mapToDrugInfoResponse(JsonNode item) {
        return DrugInfoResponse.builder()
                .itemSeq(getTextOrNull(item, "itemSeq"))
                .name(getTextOrNull(item, "itemName"))
                .efficacy(getTextOrNull(item, "efcyQesitm"))       // 효능/효과
                .useInfo(getTextOrNull(item, "useMethodQesitm"))   // 용법/용량
                .caution(getTextOrNull(item, "atpnQesitm"))        // 주의사항
                .itemImage(getTextOrNull(item, "itemImage"))       // 알약 이미지 URL
                .drugType("일반의약품")
                .build();
    }

    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode target = node.path(fieldName);
        return (target.isMissingNode() || target.isNull()) ? null : target.asText();
    }
}