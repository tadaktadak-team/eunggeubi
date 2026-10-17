package com.tadaktadak.eunggeubi.domain.emergency.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record EmergencyAlertRequest(
        @NotNull
        @DecimalMin(value = "-90.0", message = "위도가 올바르지 않습니다.")
        @DecimalMax(value = "90.0", message = "위도가 올바르지 않습니다.") Double latitude,
        @NotNull
        @DecimalMin(value = "-180.0", message = "경도가 올바르지 않습니다.")
        @DecimalMax(value = "180.0", message = "경도가 올바르지 않습니다.") Double longitude,
        String address              // 앱에서 변환한 주소. 실패하면 null
) {
}
