package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyCodeRequest(
        @NotBlank String phone,
        @NotNull Purpose purpose,
        @NotBlank String code
) {
}