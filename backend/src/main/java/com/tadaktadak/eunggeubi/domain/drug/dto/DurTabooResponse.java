package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Builder;
import lombok.Getter;

// DUR 병용금기 API(getUsjntTabooInfoList03) 응답 한 건을 그대로 옮겨 담는 상자.
// "기준 약(itemSeq)"과 "그 약과 병용금기인 상대 약(mixtureItemSeq)"이 한 쌍으로 온다.
@Getter
@Builder
public class DurTabooResponse {
    private String itemSeq;
    private String itemName;
    private String ingrKorName;        // 기준 약 성분명

    private String mixtureItemSeq;
    private String mixtureItemName;
    private String mixtureIngrKorName;  // 상대 약 성분명

    private String prohbtContent;       // 금기 사유
    private String remark;              // 비고
}
