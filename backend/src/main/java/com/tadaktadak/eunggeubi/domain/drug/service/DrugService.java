package com.tadaktadak.eunggeubi.domain.drug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugSearchPageResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugSearchSummary;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInfoRepository;
import com.tadaktadak.eunggeubi.global.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DrugInfoRepository drugInfoRepository;

    @Value("${openapi.e-drug.url}")
    private String apiUrl;

    @Value("${openapi.e-drug.service-key}")
    private String serviceKey;

    /**
     * 1. 약품명 키워드 검색. 낱알식별+e약은요+의약품 제품 허가정보가 모두 적재된 DrugInfo
     * 테이블(27,000여 건, 전문의약품 포함) 하나만 조회한다. 예전엔 e약은요를 실시간 호출해 OTC
     * 위주로만 찾고 로컬 DB로 보완했지만, 이제 로컬 DB 자체가 세 소스를 합친 상태라 실시간 외부
     * API 의존 없이 이 결과로 완결된다.
     * 목록 조회라 useInfo/caution은 안 가져오고 efficacy도 미리보기용 200자만 받는다 — 행당 평균
     * 2만 자에 달하는 LONGTEXT 컬럼들을 그대로 긁어오면 LIKE 검색(인덱스 못 타는 풀스캔)마다
     * 그 무거운 데이터까지 디스크에서 다 읽어와 응답이 10초 넘게 걸리는 문제가 실측으로 확인됨.
     */
    public DrugSearchPageResponse searchDrugsByName(String keyword, int pageNo, int numOfRows) {
        Pageable pageable = PageRequest.of(pageNo - 1, numOfRows);
        Page<DrugSearchSummary> page = drugInfoRepository.findSummaryByNameContaining(LikeEscaper.escape(keyword), pageable);

        List<DrugInfoResponse> items = page.getContent().stream()
                .map(this::toDrugInfoResponse)
                .toList();

        return DrugSearchPageResponse.builder()
                .items(items)
                .pageNo(pageNo)
                .numOfRows(numOfRows)
                .totalCount((int) page.getTotalElements())
                .build();
    }

    private DrugInfoResponse toDrugInfoResponse(DrugSearchSummary summary) {
        return DrugInfoResponse.builder()
                .itemSeq(summary.getItemSeq())
                .name(summary.getName())
                .shape(summary.getShape())
                .color(summary.getColor())
                .imprint(summary.getImprint())
                .drugType(summary.getDrugType())
                .cancelName(summary.getCancelName())
                .itemImage(summary.getItemImage())
                .efficacy(summary.getEfficacySnippet())
                .build();
    }

    /**
     * 2. 약품 상세 조회. 검색과 같은 이유로 로컬 DB 하나만 본다.
     */
    public DrugInfoResponse getDrugDetail(String itemSeq) {
        return drugInfoRepository.findById(itemSeq)
                .map(DrugInfoResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("해당 약물 정보를 찾을 수 없습니다: " + itemSeq));
    }

    /**
     * e약은요 전체를 미리 DrugInfo 테이블에 적재하는 EDrugIndexingRunner용. 검색(searchDrugsByName)과
     * 달리 itemName 없이 호출해 전체를 페이지 단위로 순회한다.
     */
    public DrugPage getAllDrugsPage(int pageNo, int numOfRows) {
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("type", "json")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .build(true)
                .toUri();

        String responseString = restTemplate.getForObject(uri, String.class);

        try {
            JsonNode rootNode = objectMapper.readTree(responseString);
            validateApiResponse(rootNode);

            JsonNode bodyNode = rootNode.path("body");
            int totalCount = bodyNode.path("totalCount").asInt(0);
            JsonNode itemsNode = bodyNode.path("items");

            List<DrugInfoResponse> items = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    items.add(mapToDrugInfoResponse(item));
                }
            }
            return new DrugPage(items, totalCount);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException("e약은요 전체 조회 중 오류가 발생했습니다.", e);
        }
    }

    public record DrugPage(List<DrugInfoResponse> items, int totalCount) {}

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
