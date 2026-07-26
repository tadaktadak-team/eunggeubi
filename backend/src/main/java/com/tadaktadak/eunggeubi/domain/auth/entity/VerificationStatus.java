package com.tadaktadak.eunggeubi.domain.auth.entity;

public enum VerificationStatus {
    PENDING,   // 인증 대기
    VERIFIED,  // 인증 완료
    EXPIRED    // 만료
}