package com.tadaktadak.eunggeubi.domain.drug.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PillSearchRequest {
    @Size(max = 20, message = "모양은 20자 이내로 입력해주세요.")
    private String drugShape;   // 모양 (예: 원형, 타원형)

    @Size(max = 20, message = "색상은 20자 이내로 입력해주세요.")
    private String colorClass;  // 색상 (예: 하양, 노랑)

    // DB 컬럼(imprint)이 50자라 그보다 긴 검색어는 어차피 결과가 없다
    @Size(max = 50, message = "식별문자는 50자 이내로 입력해주세요.")
    private String imprint;     // 식별문자(각인) 검색어

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    @Max(value = 100_000, message = "페이지 번호가 너무 큽니다.")
    private int pageNo = 1;     // 1부터 시작

    @Min(value = 1, message = "조회 건수는 1 이상이어야 합니다.")
    @Max(value = 100, message = "한 번에 최대 100건까지 조회할 수 있습니다.")
    private int numOfRows = 20; // 한 페이지 건수
}
