package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.util.List;

// isDiagnosis는 항상 false로 고정한다 (서버가 강제 - LLM이 뭐라 하든 신뢰하지 않는다).
public record RegenerateResponse(
        String message,
        boolean isDiagnosis,
        List<RegeneratedSource> sources,
        String disclaimer,
        RelatedAidGuide relatedAidGuide
) {

    public record RegeneratedSource(Long referenceSourceId, String title, String urlOrOrg) {
    }

    // ConsultationResponse.RelatedAidGuide와 같은 모양 - 체크리스트 응답까지 반영한 증상 원문에
    // 응급처치 상황이 언급됐을 때만 채워진다.
    public record RelatedAidGuide(String situation, String title) {
    }
}
