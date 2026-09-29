package com.tadaktadak.eunggeubi.domain.drug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugSearchPageResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchResponse;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInfoRepository;
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
    private final PillService pillService;
    private final DrugInfoRepository drugInfoRepository;

    @Value("${openapi.e-drug.url}")
    private String apiUrl;

    @Value("${openapi.e-drug.service-key}")
    private String serviceKey;

    /**
     * 1. 약품명 키워드 검색 API 연동 (페이지네이션)
     */
    public DrugSearchPageResponse searchDrugsByName(String keyword, int pageNo, int numOfRows) {
        List<DrugInfoResponse> resultList = new ArrayList<>();
        int totalCount = 0;

        try {
            // e약은요 API의 itemName은 등록된 약품명 문자열과의 부분일치 검색이라 공백까지 그대로 비교한다.
            // 그런데 실제 약품명(예: "어린이타이레놀산...")에는 공백이 없어서, 사용자가 "어린이 타이레놀"처럼
            // 띄어 검색하면 아예 매칭되지 않는다. 검색어의 공백을 제거해 실제 약품명 표기와 맞춰준다.
            String sanitizedKeyword = keyword.replaceAll("\\s+", "");

            // URLEncoder는 공백을 '+'로 인코딩하는데, build(true)는 이미 인코딩된 값으로 보고 그대로 전송한다.
            // 혹시 모를 공백이 남더라도 '+'가 아니라 '%20'으로 전달되도록 방어적으로 치환한다.
            String encodedKeyword = URLEncoder.encode(sanitizedKeyword, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");

            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("itemName", encodedKeyword)
                    .queryParam("type", "json")
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .build(true)
                    .toUri();

            String responseString = restTemplate.getForObject(uri, String.class);
            JsonNode rootNode = objectMapper.readTree(responseString);

            // 💡 추가된 부분: 본문을 열어보기 전에 게이트웨이/서비스 에러 검증
            validateApiResponse(rootNode);

            JsonNode bodyNode = rootNode.path("body");
            totalCount = bodyNode.path("totalCount").asInt(0);
            JsonNode itemsNode = bodyNode.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    resultList.add(mapToDrugInfoResponse(item));
                }
            }
        } catch (ExternalApiException e) {
            throw e; // 검증 로직에서 발생한 커스텀 에러는 그대로 던짐
        } catch (Exception e) {
            log.error("e약은요 Open API 검색 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("e약은요 API 연동 중 오류가 발생했습니다.", e);
        }

        return DrugSearchPageResponse.builder()
                .items(resultList)
                .pageNo(pageNo)
                .numOfRows(numOfRows)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 2. 약품 상세 조회 API 연동 (itemSeq 기반 조회)
     */
    public DrugInfoResponse getDrugDetail(String itemSeq) {
        String responseString;

        // 1) API 통신 처리
        try {
            String encodedItemSeq = URLEncoder.encode(itemSeq, StandardCharsets.UTF_8.toString());

            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("itemSeq", encodedItemSeq)
                    .queryParam("type", "json")
                    .queryParam("numOfRows", 1)
                    .build(true)
                    .toUri();

            responseString = restTemplate.getForObject(uri, String.class);
            log.debug("e약은요 API 상세조회 응답: {}", responseString);

        } catch (Exception e) {
            log.error("e약은요 Open API 상세조회 통신 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("e약은요 API 통신에 실패했습니다.", e);
        }

        // 2) 데이터 파싱 및 에러 검증 처리
        try {
            JsonNode rootNode = objectMapper.readTree(responseString);

            // 💡 추가된 부분: 본문을 열어보기 전에 게이트웨이/서비스 에러 검증
            validateApiResponse(rootNode);

            JsonNode itemsNode = rootNode.path("body").path("items");

            if (itemsNode.isArray() && !itemsNode.isEmpty()) {
                DrugInfoResponse base = mapToDrugInfoResponse(itemsNode.get(0));
                return enrichWithPillInfo(base, itemSeq);
            }
        } catch (ExternalApiException e) {
            throw e; // 검증 로직에서 발생한 커스텀 에러는 그대로 던짐
        } catch (Exception e) {
            log.error("e약은요 Open API 응답 파싱 실패: {}", e.getMessage(), e);
            throw new ExternalApiException("e약은요 API 응답 파싱 중 오류가 발생했습니다.", e);
        }

        // 3) 통신 성공했으나 검색 결과가 없는 경우
        throw new IllegalArgumentException("해당 약물 정보를 찾을 수 없습니다: " + itemSeq);
    }

    // e약은요 API에는 모양/색상/각인/전문·일반 구분 필드가 없어서 채워줘야 한다.
    // PillInfoIndexingRunner가 미리 적재해둔 DrugInfo 테이블을 먼저 보고(빠르고, 외부 API 의존 없음),
    // 배치를 아직 안 돌렸거나 새로 등록된 약이라 DB에 없으면 낱알식별 API를 실시간으로 호출해 보완한다.
    // 둘 다 실패하거나 결과가 없어도(액상/시럽처럼 애초에 낱알식별 대상이 아닌 약일 수 있음) 상세조회
    // 자체는 정상 응답하도록 예외를 여기서 흡수한다.
    private DrugInfoResponse enrichWithPillInfo(DrugInfoResponse base, String itemSeq) {
        try {
            var cached = drugInfoRepository.findById(itemSeq);
            if (cached.isPresent()) {
                DrugInfo info = cached.get();
                return base.toBuilder()
                        .shape(info.getShape())
                        .color(info.getColor())
                        .imprint(info.getImprint())
                        .drugType(info.getDrugType())
                        .build();
            }

            PillSearchResponse pill = pillService.getPillByItemSeq(itemSeq);
            if (pill == null) {
                return base;
            }
            return base.toBuilder()
                    .shape(pill.getDrugShape())
                    .color(pill.getColorClass())
                    .imprint(PillService.combineImprint(pill.getPrintFront(), pill.getPrintBack()))
                    .drugType(pill.getEtcOtcName())
                    .build();
        } catch (Exception e) {
            log.warn("낱알식별 정보 조회 실패 (itemSeq={}), 외형정보 없이 응답함", itemSeq, e);
            return base;
        }
    }

    // 💡 추가된 메서드: API 응답 에러 공통 검증 로직
    private void validateApiResponse(JsonNode rootNode) {
        // 1. 공공데이터포털 게이트웨이 에러 검사 (트래픽 초과, 잘못된 키 등)
        if (rootNode.has("OpenAPI_ServiceResponse")) {
            String errMsg = rootNode.path("OpenAPI_ServiceResponse")
                    .path("cmmMsgHeader")
                    .path("errMsg").asText();
            log.error("공공데이터포털 게이트웨이 에러: {}", errMsg);
            throw new ExternalApiException("API 게이트웨이 에러: " + errMsg, null);
        }

        // 2. 식약처 API 내부 서비스 에러 검사 (resultCode가 "00"이 아닌 경우)
        JsonNode headerNode = rootNode.path("header");
        if (!headerNode.isMissingNode() && headerNode.has("resultCode")) {
            String resultCode = headerNode.path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                String resultMsg = headerNode.path("resultMsg").asText();
                log.error("식약처 API 서비스 에러: [{}] {}", resultCode, resultMsg);
                throw new ExternalApiException("API 서비스 에러: " + resultMsg, null);
            }
        }
    }

    private DrugInfoResponse mapToDrugInfoResponse(JsonNode item) {
        return DrugInfoResponse.builder()
                .itemSeq(getTextOrNull(item, "itemSeq"))
                .name(getTextOrNull(item, "itemName"))
                .efficacy(getTextOrNull(item, "efcyQesitm"))
                .useInfo(getTextOrNull(item, "useMethodQesitm"))
                .caution(getTextOrNull(item, "atpnQesitm"))
                .itemImage(getTextOrNull(item, "itemImage"))
                // e약은요(DrbEasyDrugInfoService) 응답에는 전문/일반의약품 구분 필드가 없어서
                // "일반의약품"으로 고정 표시하면 전문의약품(예: 크라비트점안액)도 일반의약품으로 오인될 수 있다.
                // 정확한 값을 모를 땐 표시하지 않는 편이 안전하므로 채우지 않는다.
                .build();
    }

    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode target = node.path(fieldName);
        return (target.isMissingNode() || target.isNull()) ? null : target.asText();
    }
}