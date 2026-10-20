package com.tadaktadak.eunggeubi.domain.health.dto;

public record MedicationSearchItem(
        String itemSeq,
        String name,
        String drugType,   // 전문의약품/일반의약품. 모르면 null
        String itemImage
) {
}
