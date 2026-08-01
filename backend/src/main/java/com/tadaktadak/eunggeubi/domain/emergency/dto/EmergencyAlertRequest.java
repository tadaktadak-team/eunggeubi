package com.tadaktadak.eunggeubi.domain.emergency.dto;

import jakarta.validation.constraints.NotNull;

public record EmergencyAlertRequest(
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}