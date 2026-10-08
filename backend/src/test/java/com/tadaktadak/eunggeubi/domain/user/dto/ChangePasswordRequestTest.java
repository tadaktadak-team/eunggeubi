package com.tadaktadak.eunggeubi.domain.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

// 비밀번호 변경도 가입·비밀번호 재설정과 같은 정책(@ValidPassword)을 따르는지 확인한다.
class ChangePasswordRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<ChangePasswordRequest>> validate(String newPassword) {
        return validator.validate(new ChangePasswordRequest("current-pw", newPassword));
    }

    @Test
    void 정책을_지킨_비밀번호는_통과한다() {
        assertThat(validate("Ab3$kQ9z")).isEmpty();
    }

    @Test
    void 길이가_8자_미만이거나_20자_초과면_거절한다() {
        assertThat(validate("Ab3$kQ9")).isNotEmpty();
        assertThat(validate("Ab3$kQ9zAb3$kQ9zAb3$k")).isNotEmpty();   // 21자
    }

    @Test
    void 한_종류_문자만_쓰면_거절한다() {
        assertThat(validate("qwkzmxpt")).isNotEmpty();
    }

    @Test
    void 같은_문자_반복과_연속된_문자를_거절한다() {
        assertThat(validate("aaab1234x")).isNotEmpty();   // aaa
        assertThat(validate("Xk7!abcQ")).isNotEmpty();    // abc
    }

    @Test
    void 비어_있으면_거절한다() {
        assertThat(validate("")).extracting(ConstraintViolation::getMessage)
                .contains("새 비밀번호를 입력해주세요.");
    }
}
