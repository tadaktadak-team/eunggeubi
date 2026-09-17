package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendCodeRequest(
        @NotBlank String phone,
        @NotNull Purpose purpose,     // SIGNUP / FIND_ID / FIND_PW
        String email                  // FIND_PW일 때만 사용 (이메일-전화 일치 확인)
) {
}