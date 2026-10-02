package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrmsnDetailResponse {

    private String itemSeq;
    private String efficacy;   // 효능효과 (EE_DOC_DATA 파싱 결과)
    private String useInfo;    // 용법용량 (UD_DOC_DATA 파싱 결과)
    // 사용상주의사항(NB_DOC_DATA)은 평문으로 합치지 않고 최상위 ARTICLE(= "1. ...", "2. ..."
    // 같은 항목) 단위로 쪼갠 뒤 JSON 배열 문자열로 직렬화해 둔다 — 원문이 길면 항목만 수십 개,
    // 수만 자에 달해서 그대로 평문으로 보여주면 가독성이 떨어진다는 피드백에 따른 구조화.
    private String cautionSectionsJson;
    private String drugType;   // 전문/일반 구분 (ETC_OTC_CODE)
}
