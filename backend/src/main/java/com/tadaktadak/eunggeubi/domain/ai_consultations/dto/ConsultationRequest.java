package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultationRequest(@NotBlank String query) {
}
