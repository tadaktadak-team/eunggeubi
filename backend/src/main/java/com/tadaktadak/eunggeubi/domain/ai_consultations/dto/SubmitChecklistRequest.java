package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

// selectedItems는 빈 배열이어도 된다 - "체크리스트 문항 중 해당하는 게 하나도 없음"도 유효한 응답이다.
public record SubmitChecklistRequest(
        @NotNull List<String> selectedItems,
        String guestCode
) {
}
