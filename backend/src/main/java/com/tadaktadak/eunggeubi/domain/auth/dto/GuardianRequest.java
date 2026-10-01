package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import com.tadaktadak.eunggeubi.global.util.PhoneNumbers;
import com.tadaktadak.eunggeubi.global.validation.KoreanMobile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GuardianRequest(
        @NotNull Long userId,               // 회원가입 응답에서 받은 userId
        @NotBlank @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.") String name, // 보호자 이름
        @NotBlank @KoreanMobile String phone, // 보호자 연락처
        @NotNull Relationship relationship  // PARENT / GRANDPARENT / SIBLING / OTHER
) {
    public GuardianRequest {
        phone = PhoneNumbers.digitsOnly(phone);
    }
}