package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import java.time.LocalDateTime;

// 상담 이력 목록의 한 줄
public record ConsultationSummaryResponse(
        String sessionId,
        String firstQuestion,
        LocalDateTime createdAt
) {
    public static ConsultationSummaryResponse from(AiConsultation root) {
        return new ConsultationSummaryResponse(
                root.getSessionId(),
                root.getContent(),
                root.getCreatedAt()
        );
    }
}