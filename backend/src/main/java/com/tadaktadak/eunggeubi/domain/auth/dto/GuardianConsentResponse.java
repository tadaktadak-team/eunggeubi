package com.tadaktadak.eunggeubi.domain.auth.dto;

public record GuardianConsentResponse(
        String maskedPhone   // 발송 대기 화면 표시용 (예: 0101234****)
) {
}