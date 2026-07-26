package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String password,
        @NotBlank String name,
        @NotBlank String phone,
        @NotNull LocalDate birthDate,           // 프론트는 "1990-01-01" 형식으로 전송
        @NotNull Gender gender,                 // MALE / FEMALE / NONE
        String address,                         // 선택
        @AssertTrue(message = "이용약관 동의는 필수입니다.") boolean agreeService,
        @AssertTrue(message = "개인정보 처리 동의는 필수입니다.") boolean agreePrivacy,
        @AssertTrue(message = "민감정보 처리 동의는 필수입니다.") boolean agreeSensitiveInfo
) {
}