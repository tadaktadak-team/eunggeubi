package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PillSearchRequest {
    private String drugShape;   // 모양 (예: 원형, 타원형)
    private String colorClass;  // 색상 (예: 하양, 노랑)
    private String imprint;     // 식별문자(각인) 검색어
}