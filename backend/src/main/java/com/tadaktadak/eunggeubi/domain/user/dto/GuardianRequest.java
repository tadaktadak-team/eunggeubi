package com.tadaktadak.eunggeubi.domain.user.dto;

import jakarta.validation.constraints.Size;
import com.tadaktadak.eunggeubi.global.validation.KoreanMobile;
import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GuardianRequest(
        @NotBlank @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.") String name,
        @NotBlank @KoreanMobile String phone,
        @NotNull Relationship relationship,
        Boolean notifyEnabled // null이면 등록 시 기본 true
) {
}
