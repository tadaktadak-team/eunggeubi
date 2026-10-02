package com.tadaktadak.eunggeubi.domain.drug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchRequest;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchSummary;
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
public class PillService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DrugInfoRepository drugInfoRepository;

    @Value("${openapi.pill-ident.url}")
    private String apiUrl;

    @Value("${openapi.pill-ident.service-key}")
    private String serviceKey;

    /**
     * 낱알 특징(모양/색상/각인) 검색.
     * 원래는 이 API를 실시간으로 호출해서 검색했는데, 공식 문서를 확인해보니
     * DRUG_SHAPE/COLOR_CLASS1/PRINT_FRONT 같은 필드는 검색 조건(요청 파라미터)으로
     * 지원되지 않아 무슨 값을 보내도 무시되고 항상 같은 결과가 돌아왔다.
     * (지원되는 요청 파라미터: item_name, entp_name, item_seq, img_regist_ts, edi_code, bizrno)
     * 그래서 PillInfoIndexingRunner가 전체 데이터를 미리 DrugInfo 테이블에 적재해두고,
     * 검색은 그 테이블에서 하도록 바꿨다.
     */
    public List<PillSearchResponse> searchPills(PillSearchRequest request) {
        List<PillSearchSummary> results = drugInfoRepository.searchByAppearance(
                blankToNull(request.getDrugShape()),
                blankToNull(request.getColorClass()),
                blankToNull(request.getPrintFront())
        );

        return results.stream()
                .map(this::toPillSearchResponse)
                .toList();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private PillSearchResponse toPillSearchResponse(PillSearchSummary info) {
        return PillSearchResponse.builder()
                .itemSeq(info.getItemSeq())
                .itemName(info.getName())
                .entpName(info.getEntpName())
                .itemImage(info.getItemImage())
                .drugShape(info.getShape())
                .colorClass(info.getColor())
                .printFront(info.getImprint())
                .etcOtcName(info.getDrugType())
                .build();
    }

    /**
     * pageNo/numOfRows만으로 전체 목록을 페이지 단위로 가져온다. PillInfoIndexingRunner가
     * 이걸로 전체 데이터를 순회하며 DrugInfo 테이블에 적재한다. 이 API의 numOfRows 최대치는 500.
     */
    public PillPage getAllPillsPage(int pageNo, int numOfRows) {
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
            List<PillSearchResponse> items = new ArrayList<>();
            JsonNode itemsNode = bodyNode.path("items");
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    items.add(mapToPillSearchResponse(item));
                }
            }
            return new PillPage(items, totalCount);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("낱알식별 Open API 전체 목록 조회 실패 (pageNo={}): {}", pageNo, e.getMessage(), e);
            throw new ExternalApiException("낱알식별 API 연동 중 오류가 발생했습니다.", e);
        }
    }

    public record PillPage(List<PillSearchResponse> items, int totalCount) {
    }

    // 각인은 앞/뒤가 따로 내려오는데 우리 쪽에선 한 줄(imprint)로 다뤄서 합친다.
    // DrugService의 실시간 조회, PillInfoIndexingRunner의 배치 적재 둘 다 이 로직을 쓴다.
    public static String combineImprint(String printFront, String printBack) {
        boolean hasFront = printFront != null && !printFront.isBlank();
        boolean hasBack = printBack != null && !printBack.isBlank();
        if (hasFront && hasBack) return printFront + " / " + printBack;
        if (hasFront) return printFront;
        if (hasBack) return printBack;
        return null;
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