package com.tadaktadak.eunggeubi.domain.drug.dto;

// 검색 목록 전용 프로젝션. useInfo/caution은 목록 카드에 안 쓰여서 아예 제외하고,
// efficacy는 미리보기용으로 앞부분만 잘라서 받는다 — DrugInfo.efficacy/useInfo/caution이
// 의약품 제품 허가정보 적재 이후 행당 평균 2만 자에 달해서, 엔티티를 그대로 읽으면
// 검색(27,000여 건 LIKE 풀스캔)마다 이 무거운 컬럼까지 전부 디스크에서 읽어와 느려진다.
public interface DrugSearchSummary {
    String getItemSeq();
    String getName();
    String getShape();
    String getColor();
    String getImprint();
    String getDrugType();
    String getCancelName();
    String getItemImage();
    String getEfficacySnippet();
}
