package com.tadaktadak.eunggeubi.domain.drug.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Builder(toBuilder = true)
public class DrugInfoResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String itemSeq;   // 약품 코드
    private String name;      // 약품명
    private String shape;     // 모양
    private String color;     // 색상
    private String imprint;   // 각인
    private String efficacy;  // 효능/효과
    private String useInfo;   // 용법/용량
    private String caution;   // 주의사항 (평문 — e약은요 등 짧은 소스용)
    private List<CautionSection> cautionSections; // 주의사항 (항목별 구조화 — 의약품 제품 허가정보 소스용)
    private String drugType;  // 약품구분
    private String cancelName; // 허가 상태(정상/취하/유효기간만료/행정(취소) 등, null=미확인)
    private String itemImage; // 알약 이미지 URL

    //DB에서 꺼낸 Entity를 프론트엔드용 DTO 상자로 변환해주는 메서드
    public static DrugInfoResponse from(DrugInfo drugInfo) {
        return DrugInfoResponse.builder()
                .itemSeq(drugInfo.getItemSeq())
                .name(drugInfo.getName())
                .shape(drugInfo.getShape())
                .color(drugInfo.getColor())
                .imprint(drugInfo.getImprint())
                .efficacy(drugInfo.getEfficacy())
                .useInfo(drugInfo.getUseInfo())
                .caution(drugInfo.getCaution())
                .cautionSections(parseCautionSections(drugInfo.getItemSeq(), drugInfo.getCautionSections()))
                .drugType(drugInfo.getDrugType())
                .cancelName(drugInfo.getCancelName())
                .itemImage(drugInfo.getItemImage())
                .build();
    }

    private static List<CautionSection> parseCautionSections(String itemSeq, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, CautionSection.class));
        } catch (Exception e) {
            log.warn("cautionSections 파싱 실패 (itemSeq={})", itemSeq, e);
            return null;
        }
    }
}