package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;

public record SignupResponse(
        Long userId,
        UserStatus status,                  // ACTIVE(정상) 또는 PENDING(보호자동의 대기)
        boolean guardianConsentRequired,    // 만 14세 미만이면 true
        String consentToken                 // PENDING 일 때만 채움. 보호자 동의 요청/상태조회에 쓴다
) {
}