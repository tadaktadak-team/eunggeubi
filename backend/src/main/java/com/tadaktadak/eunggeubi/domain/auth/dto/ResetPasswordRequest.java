package com.tadaktadak.eunggeubi.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.tadaktadak.eunggeubi.global.validation.ValidPassword;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank @ValidPassword String newPassword
) {
}