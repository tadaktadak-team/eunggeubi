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
    // 각인 앞/뒤를 따로 안 두고 하나로 합친다. DB 기반 검색은 애초에 앞/뒤를 분리 저장하지
    // 않고(PillService.combineImprint로 합쳐서 하나의 컬럼에 저장) 프론트도 이 값을 화면에
    // 따로 보여주지 않아서, printFront/printBack 두 필드로 나눠두면 "DB 경로의 printFront는
    // 사실 합쳐진 값, 실시간 API 경로의 printFront는 진짜 앞면만"처럼 경로마다 의미가 달라지는
    // 문제만 생겼다.
    private String imprint;     // 각인(식별문자)
    private String etcOtcName;  // 전문의약품/일반의약품 구분
}