package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Builder;
import lombok.Getter;

// 선택된 약들 중 실제로 병용금기인 한 쌍. 프론트는 이 배열이 비어있으면 "병용 가능",
// 비어있지 않으면 각 항목을 "A와 B는 같이 먹으면 안 됨 - 이유" 형태로 보여주면 된다.
@Getter
@Builder
public class InteractionResponse {
    private String itemSeqA;
    private String itemNameA;
    private String itemSeqB;
    private String itemNameB;
    private String reason;
}
