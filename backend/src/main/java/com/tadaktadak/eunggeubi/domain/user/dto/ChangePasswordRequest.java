package com.tadaktadak.eunggeubi.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 이미 인증된 사용자가 비밀번호를 바꾸는 요청
public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.") String currentPassword,
        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String newPassword
) {
}