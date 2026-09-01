package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import jakarta.validation.constraints.NotBlank;

// sessionId/guestCode는 대화를 이어갈 때만 채운다. 둘 다 비어있으면 새 상담 세션으로 시작한다.
// guestCode는 비로그인 사용자만 필요 - 로그인 사용자는 JWT의 userId로 본인 확인이 되므로 null이면 된다.
public record ConsultationRequest(
        @NotBlank String query,
        String sessionId,
        String guestCode
) {
}
