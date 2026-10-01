package com.tadaktadak.eunggeubi.domain.auth.dto;

import jakarta.validation.constraints.Size;
import com.tadaktadak.eunggeubi.global.validation.KoreanMobile;
import jakarta.validation.constraints.NotBlank;

public record FindEmailRequest(
        @NotBlank @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.") String name,
        @NotBlank @KoreanMobile String phone
) {
}