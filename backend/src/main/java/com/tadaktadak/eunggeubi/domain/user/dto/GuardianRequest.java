package com.tadaktadak.eunggeubi.domain.user.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import com.tadaktadak.eunggeubi.global.util.PhoneNumbers;
import com.tadaktadak.eunggeubi.global.validation.KoreanMobile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GuardianRequest(
        @NotBlank @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.") String name,
        @NotNull(message = "전화번호를 입력해주세요.") @KoreanMobile String phone,
        @NotNull Relationship relationship,
        Boolean notifyEnabled // null이면 등록 시 기본 true
) {
    public GuardianRequest {
        phone = PhoneNumbers.digitsOnly(phone);
    }
}
