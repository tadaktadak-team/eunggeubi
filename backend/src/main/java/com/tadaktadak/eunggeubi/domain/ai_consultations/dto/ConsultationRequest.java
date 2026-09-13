package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// sessionId/guestCode는 대화를 이어갈 때만 채운다. 둘 다 비어있으면 새 상담 세션으로 시작한다.
// guestCode는 비로그인 사용자만 필요 - 로그인 사용자는 JWT의 userId로 본인 확인이 되므로 null이면 된다.
// query는 길이 제한이 없으면 임베딩·LLM 호출 비용이 그대로 뚫려서 500자로 막는다(증상 설명 용도로 충분).
public record ConsultationRequest(
        @NotBlank @Size(max = 500) String query,
        String sessionId,
        String guestCode
) {
}
