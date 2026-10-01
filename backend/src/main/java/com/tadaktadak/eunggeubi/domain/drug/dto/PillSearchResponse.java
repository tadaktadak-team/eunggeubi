package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PillSearchResponse {
    private String itemSeq;     // 품목일련번호
    private String itemName;    // 약품명
    private String entpName;    // 업체명 (제조사)
    private String itemImage;   // 알약 이미지 URL
    private String drugShape;   // 모양
    private String colorClass;  // 색상
    private String printFront;  // 각인 앞
    private String printBack;   // 각인 뒤
}