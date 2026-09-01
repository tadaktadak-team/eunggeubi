package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.util.List;

public record ChecklistDto(Long checklistId, String title, List<String> items, String status) {
}
