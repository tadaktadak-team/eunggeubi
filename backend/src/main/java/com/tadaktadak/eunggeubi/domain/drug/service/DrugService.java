package com.tadaktadak.eunggeubi.domain.drug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
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
            // ⭐ 한글 키워드를 직접 URL 인코딩 (UTF-8)
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());

            // build(true)를 유지하면서, 직접 인코딩한 키워드 삽입
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
        }

        return resultList;
    }

    /**
     * 2. 품목기준코드(itemSeq) 단건 상세 조회 API 연동
     */
    public DrugInfoResponse getDrugDetail(String itemSeq) {
        try {
            // itemSeq는 보통 숫자지만 혹시 몰라 동일하게 인코딩 처리
            String encodedItemSeq = URLEncoder.encode(itemSeq, StandardCharsets.UTF_8.toString());

            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("itemSeq", encodedItemSeq)
                    .queryParam("type", "json")
                    .build(true)
                    .toUri();

            String responseString = restTemplate.getForObject(uri, String.class);
            JsonNode rootNode = objectMapper.readTree(responseString);
            JsonNode itemsNode = rootNode.path("body").path("items");

            if (itemsNode.isArray() && !itemsNode.isEmpty()) {
                return mapToDrugInfoResponse(itemsNode.get(0));
            }
        } catch (Exception e) {
            log.error("e약은요 Open API 상세조회 실패: {}", e.getMessage(), e);
        }

        return null;
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