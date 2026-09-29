package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DrugSearchPageResponse {

    private List<DrugInfoResponse> items;
    private int pageNo;
    private int numOfRows;
    private int totalCount;
}
