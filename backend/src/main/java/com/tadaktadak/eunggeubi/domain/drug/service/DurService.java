package com.tadaktadak.eunggeubi.domain.drug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.dto.DurTabooResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.InteractionResponse;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInteraction;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInteractionRepository;
import com.tadaktadak.eunggeubi.global.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DurService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DrugInteractionRepository drugInteractionRepository;

    @Value("${openapi.dur.url}")
    private String apiUrl;

    @Value("${openapi.dur.service-key}")
    private String serviceKey;

    /**
     * 선택된 약들(itemSeq 목록) 중 서로 병용금기인 쌍을 찾는다. 실시간 API 호출 없이
     * DurInteractionIndexingRunner가 미리 적재해둔 DB만 조회한다.
     * A-B, B-A가 원본 데이터에 둘 다 있을 수 있어서 정렬된 쌍 키로 한 번만 반환한다.
     */
    public List<InteractionResponse> checkInteractions(List<String> itemSeqs) {
        if (itemSeqs == null || itemSeqs.size() < 2) {
            return List.of();
        }

        List<DrugInteraction> rows = drugInteractionRepository.findConflictsWithin(itemSeqs);

        Map<String, InteractionResponse> deduped = new LinkedHashMap<>();
        for (DrugInteraction row : rows) {
            String key = pairKey(row.getItemSeq(), row.getMixtureItemSeq());
            deduped.putIfAbsent(key, InteractionResponse.builder()
                    .itemSeqA(row.getItemSeq())
                    .itemNameA(row.getItemName())
                    .itemSeqB(row.getMixtureItemSeq())
                    .itemNameB(row.getMixtureItemName())
                    .reason(row.getProhbtContent())
                    .build());
        }
        return new ArrayList<>(deduped.values());
    }

    private String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "_" + b : b + "_" + a;
    }

    /**
     * pageNo/numOfRows만으로 전체 목록을 페이지 단위로 가져온다. DurInteractionIndexingRunner가
     * 이걸로 전체 데이터를 순회하며 DrugInteraction 테이블에 적재한다.
     * ⚠ 아직 서비스키 미등록 상태라 실제 호출/전체 건수·numOfRows 상한은 검증되지 않았다.
     * 키 받으면 낱알식별 때처럼 먼저 소량으로 테스트해볼 것.
     */
    public DurPage getAllTabooPage(int pageNo, int numOfRows) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("type", "json")
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .build(true)
                    .toUri();

            String responseString = restTemplate.getForObject(uri, String.class);
            JsonNode rootNode = objectMapper.readTree(responseString);

            validateApiResponse(rootNode);

            JsonNode bodyNode = rootNode.path("body");
            int totalCount = bodyNode.path("totalCount").asInt(0);
            List<DurTabooResponse> items = new ArrayList<>();
            JsonNode itemsNode = bodyNode.path("items");
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    items.add(mapToDurTabooResponse(item));
                }
            }
            return new DurPage(items, totalCount);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("DUR 병용금기 Open API 조회 실패 (pageNo={}): {}", pageNo, e.getMessage(), e);
            throw new ExternalApiException("DUR 병용금기 API 연동 중 오류가 발생했습니다.", e);
        }
    }

    public record DurPage(List<DurTabooResponse> items, int totalCount) {
    }

    // e약은요/낱알식별과 같은 형태의 공공데이터포털 게이트웨이/서비스 에러 검증
    private void validateApiResponse(JsonNode rootNode) {
        if (rootNode.has("OpenAPI_ServiceResponse")) {
            String errMsg = rootNode.path("OpenAPI_ServiceResponse")
                    .path("cmmMsgHeader")
                    .path("errMsg").asText();
            log.error("DUR API 게이트웨이 에러: {}", errMsg);
            throw new ExternalApiException("API 게이트웨이 에러: " + errMsg, null);
        }

        JsonNode headerNode = rootNode.path("header");
        if (!headerNode.isMissingNode() && headerNode.has("resultCode")) {
            String resultCode = headerNode.path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                String resultMsg = headerNode.path("resultMsg").asText();
                log.error("DUR API 서비스 에러: [{}] {}", resultCode, resultMsg);
                throw new ExternalApiException("API 서비스 에러: " + resultMsg, null);
            }
        }
    }

    private DurTabooResponse mapToDurTabooResponse(JsonNode item) {
        return DurTabooResponse.builder()
                .itemSeq(getTextOrNull(item, "ITEM_SEQ"))
                .itemName(getTextOrNull(item, "ITEM_NAME"))
                .ingrKorName(getTextOrNull(item, "INGR_KOR_NAME"))
                .mixtureItemSeq(getTextOrNull(item, "MIXTURE_ITEM_SEQ"))
                .mixtureItemName(getTextOrNull(item, "MIXTURE_ITEM_NAME"))
                .mixtureIngrKorName(getTextOrNull(item, "MIXTURE_INGR_KOR_NAME"))
                .prohbtContent(getTextOrNull(item, "PROHBT_CONTENT"))
                .remark(getTextOrNull(item, "REMARK"))
                .build();
    }

    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode target = node.path(fieldName);
        return (target.isMissingNode() || target.isNull()) ? null : target.asText();
    }
}
