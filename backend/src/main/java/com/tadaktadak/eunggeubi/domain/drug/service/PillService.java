package com.tadaktadak.eunggeubi.domain.drug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchRequest;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchResponse;
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
public class PillService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openapi.pill-ident.url}")
    private String apiUrl;

    @Value("${openapi.pill-ident.service-key}")
    private String serviceKey;

    public List<PillSearchResponse> searchPills(PillSearchRequest request) {
        List<PillSearchResponse> resultList = new ArrayList<>();

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("type", "json")
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 20);

            // 동적 검색 조건 처리 (한글 URLEncoder 적용)
            if (request.getDrugShape() != null && !request.getDrugShape().isBlank()) {
                builder.queryParam("DRUG_SHAPE", URLEncoder.encode(request.getDrugShape(), StandardCharsets.UTF_8.toString()));
            }
            if (request.getColorClass() != null && !request.getColorClass().isBlank()) {
                builder.queryParam("COLOR_CLASS1", URLEncoder.encode(request.getColorClass(), StandardCharsets.UTF_8.toString()));
            }
            if (request.getPrintFront() != null && !request.getPrintFront().isBlank()) {
                builder.queryParam("PRINT_FRONT", URLEncoder.encode(request.getPrintFront(), StandardCharsets.UTF_8.toString()));
            }
            if (request.getPrintBack() != null && !request.getPrintBack().isBlank()) {
                builder.queryParam("PRINT_BACK", URLEncoder.encode(request.getPrintBack(), StandardCharsets.UTF_8.toString()));
            }

            URI uri = builder.build(true).toUri();

            String responseString = restTemplate.getForObject(uri, String.class);
            JsonNode rootNode = objectMapper.readTree(responseString);
            JsonNode itemsNode = rootNode.path("body").path("items");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    resultList.add(mapToPillSearchResponse(item));
                }
            }
        } catch (Exception e) {
            log.error("낱알식별 Open API 검색 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("낱알식별 API 연동 중 오류가 발생했습니다.", e);
        }

        return resultList;
    }

    private PillSearchResponse mapToPillSearchResponse(JsonNode item) {
        return PillSearchResponse.builder()
                .itemSeq(getTextOrNull(item, "ITEM_SEQ"))
                .itemName(getTextOrNull(item, "ITEM_NAME"))
                .entpName(getTextOrNull(item, "ENTP_NAME"))
                .itemImage(getTextOrNull(item, "ITEM_IMAGE"))
                .drugShape(getTextOrNull(item, "DRUG_SHAPE"))
                .colorClass(getTextOrNull(item, "COLOR_CLASS1"))
                .printFront(getTextOrNull(item, "PRINT_FRONT"))
                .printBack(getTextOrNull(item, "PRINT_BACK"))
                .build();
    }

    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode target = node.path(fieldName);
        return (target.isMissingNode() || target.isNull()) ? null : target.asText();
    }
}