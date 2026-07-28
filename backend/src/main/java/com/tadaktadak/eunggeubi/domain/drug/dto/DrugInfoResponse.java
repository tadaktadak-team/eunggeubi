package com.tadaktadak.eunggeubi.domain.drug.dto;

import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DrugInfoResponse {

    private String itemSeq;   // 약품 코드
    private String name;      // 약품명
    private String shape;     // 모양
    private String color;     // 색상
    private String imprint;   // 각인
    private String efficacy;  // 효능/효과
    private String useInfo;   // 용법/용량
    private String caution;   // 주의사항
    private String drugType;  // 약품구분

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
                .drugType(drugInfo.getDrugType())
                .build();
    }
}