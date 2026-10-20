package com.tadaktadak.eunggeubi.domain.user.dto;

import jakarta.validation.constraints.Size;
import com.tadaktadak.eunggeubi.global.validation.KoreanMobile;
import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateMyInfoRequest(
        @NotBlank(message = "이름을 입력해주세요.") @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.") String name,
        @NotBlank(message = "전화번호를 입력해주세요.") @KoreanMobile String phone,
        @NotNull(message = "생년월일을 입력해주세요.") LocalDate birthDate,
        @NotNull(message = "성별을 선택해주세요.") Gender gender,
        @Size(max = 200, message = "주소는 200자 이내로 입력해주세요.") String address
) {
}
