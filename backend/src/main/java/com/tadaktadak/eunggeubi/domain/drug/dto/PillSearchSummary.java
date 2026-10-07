package com.tadaktadak.eunggeubi.domain.drug.dto;

// 낱알 특징(모양/색상/각인) 검색 전용 프로젝션. DrugSearchSummary와 같은 이유로, 흔한 모양
// 하나만 조건으로 줘도 수천~수만 건이 매칭될 수 있어(예: "원형" 9,837건) efficacy/useInfo/
// caution/cautionSections 같은 무거운 LONGTEXT 컬럼은 아예 빼고 필요한 필드만 받는다.
public interface PillSearchSummary {
    String getItemSeq();
    String getName();
    String getEntpName();
    String getShape();
    String getColor();
    String getImprint();
    String getDrugType();
    String getItemImage();
}
