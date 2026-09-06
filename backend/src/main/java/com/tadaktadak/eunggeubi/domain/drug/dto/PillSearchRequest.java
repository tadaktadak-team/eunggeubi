package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PillSearchRequest {
    private String drugShape;   // 모양 (예: 원형, 타원형)
    private String colorClass;  // 색상 (예: 하양, 노랑)
    private String printFront;  // 각인 앞면
    private String printBack;   // 각인 뒷면
}