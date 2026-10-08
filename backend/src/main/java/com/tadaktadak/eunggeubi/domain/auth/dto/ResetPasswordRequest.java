package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.global.validation.KoreanMobile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.tadaktadak.eunggeubi.global.validation.ValidPassword;

public record ResetPasswordRequest(
        @NotBlank @Email @Size(max = 100, message = "이메일은 100자 이내로 입력해주세요.") String email,
        @NotBlank @KoreanMobile String phone,
        @NotBlank @ValidPassword String newPassword   // 길이(8~20)를 포함한 비밀번호 정책은 ValidPassword 가 검사한다
) {
}
