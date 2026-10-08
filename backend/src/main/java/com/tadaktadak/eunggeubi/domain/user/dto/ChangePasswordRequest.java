package com.tadaktadak.eunggeubi.domain.user.dto;

import com.tadaktadak.eunggeubi.global.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

// 이미 인증된 사용자가 비밀번호를 바꾸는 요청
public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.") String currentPassword,
        // 가입·비밀번호 재설정과 같은 정책(길이 8~20 포함). 이메일·전화번호 포함 여부는 서비스에서 따로 검사한다
        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @ValidPassword String newPassword
) {
}
