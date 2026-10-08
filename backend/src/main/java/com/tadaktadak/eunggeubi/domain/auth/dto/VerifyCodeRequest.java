package com.tadaktadak.eunggeubi.domain.auth.dto;

import jakarta.validation.constraints.Size;
import com.tadaktadak.eunggeubi.global.validation.KoreanMobile;
import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyCodeRequest(
        @NotBlank @KoreanMobile String phone,
        @NotNull Purpose purpose,
        @NotBlank @Size(max = 10, message = "인증번호가 올바르지 않습니다.") String code
) {
}