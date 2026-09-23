package com.tadaktadak.eunggeubi.domain.emergency.dto;

import jakarta.validation.constraints.NotNull;

public record EmergencyAlertRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        String address              // 앱에서 변환한 주소. 실패하면 null
) {
}