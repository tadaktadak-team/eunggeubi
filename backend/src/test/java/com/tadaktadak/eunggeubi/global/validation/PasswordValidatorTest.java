package com.tadaktadak.eunggeubi.global.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tadaktadak.eunggeubi.domain.auth.dto.ResetPasswordRequest;
import com.tadaktadak.eunggeubi.domain.auth.dto.SignupRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("비밀번호 정책")
class PasswordValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    /** DTO 전체를 만들지 않고 비밀번호 규칙만 보기 위한 최소 홀더 */
    record PasswordHolder(@ValidPassword String password) {}

    /** 위반 메시지를 반환. 통과하면 null */
    private String violationOf(String password) {
        Set<ConstraintViolation<PasswordHolder>> violations =
                validator.validate(new PasswordHolder(password));
        return violations.isEmpty() ? null : violations.iterator().next().getMessage();
    }

    @Nested
    @DisplayName("통과하는 비밀번호")
    class Valid {

        @ParameterizedTest
        @ValueSource(strings = {
                "Pw9!kQ2m",             // 8자 경계, 3종 조합
                "hunter2024",           // 영문+숫자 2종
                "a1!a1!a1!a1!a1!a1!a1", // 20자 경계
                "aa11!!bb22",           // 같은 문자 2회 연속은 허용
                "acegik24",             // 한 칸씩 건너뛰면 연속 아님
        })
        void 규칙을_모두_만족하면_통과한다(String password) {
            assertThat(violationOf(password)).isNull();
        }

        @Test
        void null은_NotBlank가_담당하므로_통과시킨다() {
            assertThat(violationOf(null)).isNull();
        }
    }

    @Nested
    @DisplayName("길이")
    class Length {

        @ParameterizedTest
        @ValueSource(strings = {
                "Pw9!kQ",                // 6자
                "Pw9!kQ2",               // 7자 (경계 바로 아래)
                "a1!a1!a1!a1!a1!a1!a1x", // 21자 (경계 바로 위)
        })
        void 길이를_벗어나면_막힌다(String password) {
            assertThat(violationOf(password)).contains("8~20자");
        }
    }

    @Nested
    @DisplayName("허용 문자")
    class Charset {

        @ParameterizedTest
        @ValueSource(strings = {
                "비밀번호1234!",  // 한글
                "Pw9 kQ2m",       // 공백
        })
        void 영문_숫자_특수문자_외에는_막힌다(String password) {
            assertThat(violationOf(password)).contains("영문, 숫자, 특수문자만");
        }
    }

    @Nested
    @DisplayName("조합")
    class Combination {

        @ParameterizedTest
        @ValueSource(strings = {
                "acegikmo",  // 영문만
                "13570246",  // 숫자만
                "!@#$%^&*",  // 특수문자만
        })
        void 한_종류만_쓰면_막힌다(String password) {
            assertThat(violationOf(password)).contains("2종류 이상");
        }
    }

    @Nested
    @DisplayName("동일 문자 연속")
    class Repeat {

        @ParameterizedTest
        @ValueSource(strings = {
                "Paaa12!q",  // aaa
                "Pw111k!q",  // 111
                "Pw!!!kQ2",  // !!!
        })
        void 같은_문자_3회_연속은_막힌다(String password) {
            assertThat(violationOf(password)).contains("연속해서");
        }

        @Test
        void 같은_문자_2회_연속은_허용한다() {
            assertThat(violationOf("Paa12!qw")).isNull();
        }
    }

    @Nested
    @DisplayName("연속된 문자열")
    class Sequence {

        @ParameterizedTest
        @ValueSource(strings = {
                "Pabc12!q",  // abc 오름차순
                "Pcba12!q",  // cba 내림차순
                "Pw!k123x",  // 123 오름차순
                "Pw!k321x",  // 321 내림차순
        })
        void 연속_3자는_막힌다(String password) {
            assertThat(violationOf(password)).contains("연속된 문자나 숫자");
        }

        @Test
        void 숫자와_특수문자가_붙어도_연속으로_보지_않는다() {
            // '9'(57)와 ':'(58)은 코드값이 1 차이지만 종류가 달라 연속이 아님
            assertThat(violationOf("Pw!kQ29:")).isNull();
        }
    }

    @Nested
    @DisplayName("개인정보 포함 금지")
    class UserInfo {

        @Test
        void 이메일_아이디가_들어가면_막힌다() {
            assertThatThrownBy(() -> PasswordValidator.ensureNotContainsUserInfo(
                    "hong1234!A", "hong1234@test.com", "01012345678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 아이디");
        }

        @Test
        void 전화번호_조각이_들어가면_막힌다() {
            assertThatThrownBy(() -> PasswordValidator.ensureNotContainsUserInfo(
                    "Pw5678!kQ", "hong@test.com", "010-1234-5678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("전화번호");
        }

        @Test
        void 대소문자가_달라도_이메일_아이디를_잡는다() {
            assertThatThrownBy(() -> PasswordValidator.ensureNotContainsUserInfo(
                    "HONG1234!A", "hong1234@test.com", "01012345678"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 겹치지_않으면_통과한다() {
            assertThatCode(() -> PasswordValidator.ensureNotContainsUserInfo(
                    "Pw9!kQ2m", "hong@test.com", "010-1234-5678"))
                    .doesNotThrowAnyException();
        }

        @Test
        void 이메일이나_전화번호가_null이어도_터지지_않는다() {
            assertThatCode(() -> PasswordValidator.ensureNotContainsUserInfo(
                    "Pw9!kQ2m", null, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("DTO 연결")
    class DtoWiring {

        @Test
        void SignupRequest의_password에_규칙이_걸려있다() {
            assertThat(validator.validateValue(SignupRequest.class, "password", "aaa"))
                    .isNotEmpty();
        }

        @Test
        void ResetPasswordRequest의_newPassword에_규칙이_걸려있다() {
            assertThat(validator.validateValue(ResetPasswordRequest.class, "newPassword", "aaa"))
                    .isNotEmpty();
        }
    }
}