package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 소셜 신규 가입 완료 요청. 약관 동의(+부족 정보 입력) 화면에서 보낸다.
 * @AssertTrue = 반드시 true(동의)여야 통과. 하나라도 false면 400.
 * phone/birthDate/gender = 제공자가 안 준 필수정보를 앱에서 받아 채울 때만 사용(카카오). 네이버는 null로 옴.
 */
public record SocialSignupCompleteRequest(
        @NotBlank String ticket,
        @AssertTrue(message = "이용약관 동의는 필수입니다.") boolean agreeService,
        @AssertTrue(message = "개인정보 처리 동의는 필수입니다.") boolean agreePrivacy,
        @AssertTrue(message = "민감정보 처리 동의는 필수입니다.") boolean agreeSensitiveInfo,
        String phone,
        LocalDate birthDate,
        Gender gender
) {
}