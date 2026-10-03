package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GuardianRequest(
        @NotBlank String consentToken,      // 회원가입 응답에서 받은 보호자 동의 전용 토큰
        @NotBlank String name,              // 보호자 이름
        @NotBlank String phone,             // 보호자 연락처
        @NotNull Relationship relationship  // PARENT / GRANDPARENT / SIBLING / OTHER
) {
}