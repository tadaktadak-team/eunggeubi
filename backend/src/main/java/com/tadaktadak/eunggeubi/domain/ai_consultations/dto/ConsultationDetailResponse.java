package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.time.LocalDateTime;
import java.util.List;

//상담 한 건의 전체 대화.
public record ConsultationDetailResponse(
        String sessionId,
        List<Message> messages
) {
    public record Message(
            Long id,
            String senderType,
            String content,
            boolean regenerated,
            LocalDateTime createdAt,
            List<String> checkedItems //없으면 null
    ) {
    }
}