package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 낱알 특징 검색 결과 한 페이지. 약품명 검색(DrugSearchPageResponse)과 같은 형태로 맞춰서
// 프론트가 두 검색에서 같은 방식(무한 스크롤)으로 이어 받을 수 있게 한다.
@Getter
@Builder
public class PillSearchPageResponse {
    private List<PillSearchResponse> items;
    private int pageNo;
    private int numOfRows;
    private int totalCount;
}
