package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.time.LocalDateTime;
import java.util.List;

//상담 한 건의 전체 대화.
public record ConsultationDetailResponse(
        String sessionId,
        List<Message> messages
) {
    // disclaimer는 AI 메시지에만 채운다(사용자 메시지는 null) - consult/regenerate 응답과 같은 문구를
    // ConsultationDisclaimer에서 그대로 가져온다. DB엔 저장 안 하고 조회 시점에 붙인다.
    public record Message(
            Long id,
            String senderType,
            String content,
            boolean regenerated,
            LocalDateTime createdAt,
            List<String> checkedItems,
            boolean checklistAnswered,
            String disclaimer
    ) {
    }
}