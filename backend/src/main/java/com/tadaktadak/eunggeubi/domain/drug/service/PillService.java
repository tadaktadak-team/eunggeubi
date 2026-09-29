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

            //body 파싱 전에 게이트웨이 / 서비스 에러 검증
            validateApiResponse(rootNode);

            JsonNode itemsNode = rootNode.path("body").path("items");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    resultList.add(mapToPillSearchResponse(item));
                }
            }
        } catch (ExternalApiException e) {
            throw e; //검증 로직에서 직접 던진 예외는 그대로 전달
        } catch (Exception e) {
            log.error("낱알식별 Open API 검색 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("낱알식별 API 연동 중 오류가 발생했습니다.", e);
        }

        return resultList;
    }

    /**
     * item_seq(소문자)로 특정 약의 외형 정보를 단건 조회
     * 약물 상세화면에서 e약은요 API 결과에 모양/색상/각인/전문·일반 구분을 덧붙이기 위해 씀
     * 이 API는 DRUG_SHAPE 같은 검색 필터는 대문자인데, 단건조회 파라미터만 소문자 item_seq
     * 대문자 ITEM_SEQ로 넣으면 필터로 인식되지 않고 조건 없이 전체 목록이 돌아옴
     */
    public PillSearchResponse getPillByItemSeq(String itemSeq) {
        try {
            String encodedItemSeq = URLEncoder.encode(itemSeq, StandardCharsets.UTF_8.toString());

            URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("type", "json")
                    .queryParam("item_seq", encodedItemSeq)
                    .build(true)
                    .toUri();

            String responseString = restTemplate.getForObject(uri, String.class);
            JsonNode rootNode = objectMapper.readTree(responseString);

            validateApiResponse(rootNode);

            JsonNode itemsNode = rootNode.path("body").path("items");
            if (itemsNode.isArray() && !itemsNode.isEmpty()) {
                return mapToPillSearchResponse(itemsNode.get(0));
            }
            // 낱알식별 DB에 없는 약(액상/시럽 등)일 수 있어서 결과 없음은 에러가 아니라 정상 케이스
            return null;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("낱알식별 Open API 단건조회 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("낱알식별 API 연동 중 오류가 발생했습니다.", e);
        }
    }

    //공통 API 응답 에러 검증 메서드
    private void validateApiResponse(JsonNode rootNode) {
        // 1. 공공데이터포털 게이트웨이 에러 검사
        if (rootNode.has("OpenAPI_ServiceResponse")) {
            String errMsg = rootNode.path("OpenAPI_ServiceResponse")
                    .path("cmmMsgHeader")
                    .path("errMsg").asText();
            log.error("낱알식별 API 게이트웨이 에러: {}", errMsg);
            throw new ExternalApiException("API 게이트웨이 에러: " + errMsg, null);
        }

        // 2. 식약처 API 내부 서비스 에러 검사 (resultCode가 "00"이 아닌 경우)
        JsonNode headerNode = rootNode.path("header");
        if (!headerNode.isMissingNode() && headerNode.has("resultCode")) {
            String resultCode = headerNode.path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                String resultMsg = headerNode.path("resultMsg").asText();
                log.error("낱알식별 API 서비스 에러: [{}] {}", resultCode, resultMsg);
                throw new ExternalApiException("API 서비스 에러: " + resultMsg, null);
            }
        }
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
                .etcOtcName(getTextOrNull(item, "ETC_OTC_NAME"))
                .build();
    }

    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode target = node.path(fieldName);
        return (target.isMissingNode() || target.isNull()) ? null : target.asText();
    }
}