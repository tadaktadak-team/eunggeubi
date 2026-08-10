package com.tadaktadak.eunggeubi.domain.user.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GuardianRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @NotNull Relationship relationship,
        Boolean notifyEnabled // null이면 등록 시 기본 true
) {
}
