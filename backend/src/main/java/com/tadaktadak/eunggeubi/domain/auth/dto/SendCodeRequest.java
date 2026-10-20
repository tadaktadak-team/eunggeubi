package com.tadaktadak.eunggeubi.domain.auth.dto;

import jakarta.validation.constraints.Size;
import com.tadaktadak.eunggeubi.global.validation.KoreanMobile;
import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendCodeRequest(
        @NotBlank @KoreanMobile String phone,
        @NotNull Purpose purpose,     // SIGNUP / FIND_ID / FIND_PW
        @Size(max = 100, message = "이메일은 100자 이내로 입력해주세요.") String email // FIND_PW일 때만 사용 (이메일-전화 일치 확인)
) {
}