package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendCodeRequest(
        @NotBlank String phone,
        @NotNull Purpose purpose      // SIGNUP(회원가입) / FIND_PW(비번찾기)
) {
}