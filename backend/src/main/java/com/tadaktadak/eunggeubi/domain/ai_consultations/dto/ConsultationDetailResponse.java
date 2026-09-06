package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import java.time.LocalDateTime;
import java.util.List;

// 상담 한 건의 전체 대화. 시간순으로 USER/AI 메시지가 번갈아 담긴다.
public record ConsultationDetailResponse(
        String sessionId,
        List<Message> messages
) {
    public record Message(
            Long id,
            String senderType,
            String content,
            boolean regenerated,
            LocalDateTime createdAt
    ) {
        static Message from(AiConsultation consultation) {
            return new Message(
                    consultation.getId(),
                    consultation.getSenderType().name(),
                    consultation.getContent(),
                    consultation.isRegenerated(),
                    consultation.getCreatedAt()
            );
        }
    }

    public static ConsultationDetailResponse from(String sessionId, List<AiConsultation> messages) {
        return new ConsultationDetailResponse(
                sessionId,
                messages.stream().map(Message::from).toList()
        );
    }
}