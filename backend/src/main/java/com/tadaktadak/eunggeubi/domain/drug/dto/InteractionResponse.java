package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 선택된 약들 중 실제로 병용금기인 한 쌍. 프론트는 이 배열이 비어있으면 "병용금기 목록에는
// 해당하지 않음", 비어있지 않으면 각 항목을 "A와 B는 같이 먹으면 안 됨 - 이유들" 형태로 보여주면 된다.
// 같은 쌍이 성분별로 여러 행(서로 다른 사유)으로 들어있는 경우가 많아서(실측: 14만 쌍 이상)
// 사유는 하나가 아니라 목록으로 내려준다.
@Getter
@Builder
public class InteractionResponse {
    private String itemSeqA;
    private String itemNameA;
    private String itemSeqB;
    private String itemNameB;
    private List<String> reasons;
}
