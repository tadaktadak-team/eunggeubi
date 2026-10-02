package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 의약품 제품 허가정보 API의 사용상주의사항(NB_DOC_DATA) 원문이 "1. 다음 환자에는 투여하지
// 말 것", "2. 이상반응"처럼 최상위 ARTICLE 단위로 항목이 나뉘어 있어서, 그 구조를 그대로
// 살려 항목별로 쪼갠 단위. title이 없는 경우(구조를 못 찾았을 때의 폴백)는 null.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CautionSection {
    private String title;
    private String body;
}
