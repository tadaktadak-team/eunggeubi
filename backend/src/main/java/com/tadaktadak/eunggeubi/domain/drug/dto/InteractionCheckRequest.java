package com.tadaktadak.eunggeubi.domain.drug.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InteractionCheckRequest {
    // 한 번에 확인할 약은 20개까지(건강 프로필의 복용약 상한과 같다). 쌍의 수는 개수의 제곱으로 늘어나 DB 조회가 무거워진다.
    // 품목번호는 DrugController 의 상세 조회와 같은 형식(숫자 1~20자)만 받는다.
    @NotNull(message = "약 목록이 필요합니다.")
    @Size(max = 20, message = "한 번에 최대 20개까지 확인할 수 있습니다.")
    private List<@NotBlank(message = "약품 번호가 올바르지 않습니다.")
                 @Pattern(regexp = "^\\d{1,20}$", message = "약품 번호가 올바르지 않습니다.") String> itemSeqs; // 사용자가 화면에서 고른 약들의 itemSeq 목록
}
