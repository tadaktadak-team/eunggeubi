package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.util.List;

// isDiagnosis는 항상 false로 고정한다 (서버가 강제 - LLM이 뭐라 하든 신뢰하지 않는다).
public record RegenerateResponse(
        String message,
        boolean isDiagnosis,
        List<RegeneratedSource> sources,
        String disclaimer
) {

    public record RegeneratedSource(Long referenceSourceId, String title, String urlOrOrg) {
    }
}
