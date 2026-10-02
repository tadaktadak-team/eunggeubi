package com.tadaktadak.eunggeubi.domain.drug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.dto.CautionSection;
import com.tadaktadak.eunggeubi.domain.drug.dto.PrmsnDetailResponse;
import com.tadaktadak.eunggeubi.global.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

// 의약품 제품 허가정보(DrugPrdtPrmsnInfoService08). e약은요는 "일반의약품 자가복용 안내" 목적이라
// 전문의약품 데이터가 아예 없는데(실측: 전문의약품 19,813건 전부 효능정보 0건), 이 API는 허가받은
// 의약품 전체(전문/일반 구분 없이, 총 42,707건)의 첨부문서 원문을 제공해 그 공백을 메울 수 있다.
// 다만 응답 텍스트가 평문이 아니라 <DOC><SECTION><ARTICLE><PARAGRAPH> 형태의 중첩 마크업이고,
// 약마다 실제 내용이 ARTICLE의 title 속성에 들어있거나 PARAGRAPH의 CDATA 안에 들어있는 등
// 구조가 일정하지 않아 DOM으로 파싱해 leaf 노드 기준으로 텍스트를 복원한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class DrugPrmsnService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openapi.drug-prmsn.url}")
    private String apiUrl;

    @Value("${openapi.drug-prmsn.service-key}")
    private String serviceKey;

    /**
     * item_seq 없이 호출해 전체를 페이지 단위로 순회한다. DrugPrmsnIndexingRunner가 이걸로
     * 기존 DrugInfo 중 효능/용법/주의/구분이 비어있는 행만 채운다(새 행은 추가하지 않음).
     */
    public PrmsnPage getAllPrmsnDetailPage(int pageNo, int numOfRows) {
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
            JsonNode itemsNode = bodyNode.path("items");

            List<PrmsnDetailResponse> items = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    items.add(mapToPrmsnDetailResponse(item));
                }
            }
            return new PrmsnPage(items, totalCount);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("의약품 제품 허가정보 API 조회 실패 (pageNo={}): {}", pageNo, e.getMessage(), e);
            throw new ExternalApiException("의약품 제품 허가정보 API 연동 중 오류가 발생했습니다.", e);
        }
    }

    public record PrmsnPage(List<PrmsnDetailResponse> items, int totalCount) {}

    private void validateApiResponse(JsonNode rootNode) {
        if (rootNode.has("OpenAPI_ServiceResponse")) {
            String errMsg = rootNode.path("OpenAPI_ServiceResponse")
                    .path("cmmMsgHeader")
                    .path("errMsg").asText();
            log.error("의약품 제품 허가정보 API 게이트웨이 에러: {}", errMsg);
            throw new ExternalApiException("API 게이트웨이 에러: " + errMsg, null);
        }

        JsonNode headerNode = rootNode.path("header");
        if (!headerNode.isMissingNode() && headerNode.has("resultCode")) {
            String resultCode = headerNode.path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                String resultMsg = headerNode.path("resultMsg").asText();
                log.error("의약품 제품 허가정보 API 서비스 에러: [{}] {}", resultCode, resultMsg);
                throw new ExternalApiException("API 서비스 에러: " + resultMsg, null);
            }
        }
    }

    private PrmsnDetailResponse mapToPrmsnDetailResponse(JsonNode item) {
        return PrmsnDetailResponse.builder()
                .itemSeq(getTextOrNull(item, "ITEM_SEQ"))
                .efficacy(extractDocText(getTextOrNull(item, "EE_DOC_DATA")))
                .useInfo(extractDocText(getTextOrNull(item, "UD_DOC_DATA")))
                .cautionSectionsJson(extractCautionSectionsJson(getTextOrNull(item, "NB_DOC_DATA")))
                .drugType(getTextOrNull(item, "ETC_OTC_CODE"))
                .build();
    }

    // NB_DOC_DATA(사용상주의사항)는 최상위 ARTICLE 단위로 "1. 다음 환자에는 투여하지 말 것",
    // "2. 이상반응"처럼 항목이 이미 나뉘어 있다(실측 확인). 이 구조를 그대로 살려 항목별
    // {title, body} 목록으로 반환하고, 프론트에서 항목별 접기/펼치기로 보여줄 수 있게 한다.
    private String extractCautionSectionsJson(String xmlFragment) {
        if (xmlFragment == null || xmlFragment.isBlank()) {
            return null;
        }
        List<CautionSection> sections = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlFragment)));

            collectSections(doc.getDocumentElement(), sections);
        } catch (Exception e) {
            log.warn("사용상주의사항 구조 파싱 실패, 평문 폴백으로 대체", e);
        }

        if (sections.isEmpty()) {
            // 구조를 못 찾았거나 ARTICLE title이 전부 비어있는 경우 — 기존 평문 추출로 폴백해
            // 최소한 내용 자체는 잃지 않게 한다(제목 없는 단일 섹션 취급).
            String whole = extractDocText(xmlFragment);
            if (whole == null) {
                return null;
            }
            sections.add(CautionSection.builder().title(null).body(whole).build());
        }

        try {
            return objectMapper.writeValueAsString(sections);
        } catch (Exception e) {
            log.warn("사용상주의사항 섹션 직렬화 실패", e);
            return null;
        }
    }

    // DOC/SECTION은 래퍼일 뿐이라 한 단계 더 들어가고, 최상위 ARTICLE 하나를 섹션 하나로 취급한다.
    // ARTICLE이 더 깊은 하위 ARTICLE을 갖는 경우는 없다고 실측으로 확인돼 한 단계만 다룬다.
    private void collectSections(Element parent, List<CautionSection> sections) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) node;
            if ("SECTION".equalsIgnoreCase(el.getTagName())) {
                collectSections(el, sections);
                continue;
            }
            if (!"ARTICLE".equalsIgnoreCase(el.getTagName())) {
                continue;
            }

            String title = decodeHtmlEntities(el.getAttribute("title"));
            List<String> bodyLines = new ArrayList<>();
            // isRoot=true로 호출해 이 ARTICLE 자신의 title은 본문에 다시 섞이지 않고,
            // 자식(PARAGRAPH 등)의 내용만 모은다.
            collectLeafText(el, bodyLines, true);
            String body = String.join("\n", bodyLines).trim();

            boolean hasTitle = title != null && !title.isBlank();
            if (!hasTitle && body.isBlank()) {
                continue; // 제목도 내용도 없으면 빈 섹션이라 건너뜀
            }
            // 제목만 있고 본문이 비어있는 경우(제목 자체가 유일한 내용인 구조)는 제목을 본문으로도 씀
            sections.add(CautionSection.builder()
                    .title(hasTitle ? title : null)
                    .body(body.isBlank() ? title : body)
                    .build());
        }
    }

    // <DOC title="효능효과" type="EE"><SECTION><ARTICLE title="..."/>...</SECTION></DOC> 형태를
    // 평문으로 복원한다. 실제 내용이 어느 레벨에 들어있는지 약마다 달라서(ARTICLE의 title 속성이거나
    // PARAGRAPH의 텍스트/CDATA이거나) 더 이상 자식 엘리먼트가 없는 leaf 노드를 전부 찾아 title 속성이나
    // 텍스트 내용 중 비어있지 않은 쪽을 순서대로 모은다. 최상위 DOC 자체는 "효능효과" 같은 라벨일
    // 뿐이라 제외한다.
    private String extractDocText(String xmlFragment) {
        if (xmlFragment == null || xmlFragment.isBlank()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlFragment)));

            List<String> lines = new ArrayList<>();
            collectLeafText(doc.getDocumentElement(), lines, true);
            String joined = String.join("\n", lines).trim();
            return joined.isEmpty() ? null : joined;
        } catch (Exception e) {
            // 파싱 실패해도 전체를 버리지 않고 태그만 벗겨낸 텍스트로 최대한 복구한다.
            String fallback = decodeHtmlEntities(xmlFragment.replaceAll("<!\\[CDATA\\[(.*?)]]>", "$1")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " "))
                    .trim();
            return fallback.isEmpty() ? null : fallback;
        }
    }

    private void collectLeafText(Element el, List<String> lines, boolean isRoot) {
        NodeList children = el.getChildNodes();
        boolean hasElementChild = false;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                hasElementChild = true;
                collectLeafText((Element) children.item(i), lines, false);
            }
        }
        if (hasElementChild || isRoot) {
            return;
        }
        String text = decodeHtmlEntities(el.getTextContent());
        if (text != null && !text.isBlank()) {
            lines.add(text);
            return;
        }
        String title = decodeHtmlEntities(el.getAttribute("title"));
        if (title != null && !title.isBlank()) {
            lines.add(title);
        }
    }

    // 원문이 "&nbsp;"처럼 실제 XML 엔티티가 아니라 CDATA 안에 리터럴 텍스트로 박혀있는 HTML
    // 엔티티를 담고 있는 경우가 있어(빈 문단을 표시하는 용도로 보임) 직접 치환한다.
    private String decodeHtmlEntities(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .trim();
    }

    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode target = node.path(fieldName);
        return (target.isMissingNode() || target.isNull()) ? null : target.asText();
    }
}
