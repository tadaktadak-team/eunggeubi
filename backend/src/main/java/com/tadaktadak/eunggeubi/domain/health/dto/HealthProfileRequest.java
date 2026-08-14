package com.tadaktadak.eunggeubi.domain.health.dto;

import java.util.List;

public record HealthProfileRequest(
        String bloodType, //혈액형
        List<String> diseases, //병명
        List<String> medications //약명
) {
}