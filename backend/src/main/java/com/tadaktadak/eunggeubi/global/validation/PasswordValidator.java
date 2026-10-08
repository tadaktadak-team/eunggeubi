package com.tadaktadak.eunggeubi.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 20;
    private static final int MAX_REPEAT = 3;    // 동일 문자 3회 연속부터 차단
    private static final int MAX_SEQUENCE = 3;  // 연속 문자열 3자부터 차단
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?~`";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // null·빈 값은 @NotBlank가 담당. 여기서 통과시켜야 에러 메시지가 중복되지 않음
        if (password == null || password.isBlank()) {
            return true;
        }

        String error = findViolation(password);
        if (error == null) {
            return true;
        }

        // 기본 메시지 대신 규칙별 메시지로 교체
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }

    /** 위반한 규칙의 메시지를 반환. 통과하면 null */
    private String findViolation(String password) {
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return "비밀번호는 %d~%d자여야 합니다.".formatted(MIN_LENGTH, MAX_LENGTH);
        }
        if (!isAllowedCharset(password)) {
            return "비밀번호는 영문, 숫자, 특수문자만 사용할 수 있습니다.";
        }
        if (countCharTypes(password) < 2) {
            return "비밀번호는 영문, 숫자, 특수문자 중 2종류 이상을 조합해야 합니다.";
        }
        if (hasRepeatedChars(password)) {
            return "같은 문자를 %d번 이상 연속해서 사용할 수 없습니다.".formatted(MAX_REPEAT);
        }
        if (hasSequentialChars(password)) {
            return "연속된 문자나 숫자를 %d자 이상 사용할 수 없습니다.".formatted(MAX_SEQUENCE);
        }
        return null;
    }

    private boolean isAllowedCharset(String password) {
        for (char c : password.toCharArray()) {
            if (!isAlpha(c) && !isDigit(c) && !isSpecial(c)) {
                return false;
            }
        }
        return true;
    }

    private int countCharTypes(String password) {
        boolean alpha = false, digit = false, special = false;
        for (char c : password.toCharArray()) {
            if (isAlpha(c)) alpha = true;
            else if (isDigit(c)) digit = true;
            else if (isSpecial(c)) special = true;
        }
        int count = 0;
        if (alpha) count++;
        if (digit) count++;
        if (special) count++;
        return count;
    }

    /** aaa, 111 처럼 같은 문자가 MAX_REPEAT번 연속되는지 */
    private boolean hasRepeatedChars(String password) {
        int run = 1;
        for (int i = 1; i < password.length(); i++) {
            run = (password.charAt(i) == password.charAt(i - 1)) ? run + 1 : 1;
            if (run >= MAX_REPEAT) {
                return true;
            }
        }
        return false;
    }

    /** abc, 123 (오름차순) / cba, 321 (내림차순) 둘 다 차단 */
    private boolean hasSequentialChars(String password) {
        int ascending = 1, descending = 1;
        for (int i = 1; i < password.length(); i++) {
            char prev = password.charAt(i - 1);
            char curr = password.charAt(i);

            // 같은 문자 종류끼리만 연속으로 판단 (예: '9'→':' 를 연속으로 보지 않도록)
            boolean sameType = (isAlpha(prev) && isAlpha(curr)) || (isDigit(prev) && isDigit(curr));
            int diff = sameType ? curr - prev : 0;

            ascending = (diff == 1) ? ascending + 1 : 1;
            descending = (diff == -1) ? descending + 1 : 1;
            if (ascending >= MAX_SEQUENCE || descending >= MAX_SEQUENCE) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isSpecial(char c) {
        return SPECIAL_CHARS.indexOf(c) >= 0;
    }

    private static final int MIN_TOKEN_LENGTH = 4;  // 너무 짧은 조각은 오탐이라 제외

    /**
     * 비밀번호에 이메일 아이디나 전화번호 일부가 들어있으면 예외.
     * 필드 레벨 검증으로는 다른 필드를 볼 수 없어서 서비스에서 호출한다.
     */
    public static void ensureNotContainsUserInfo(String password, String email, String phone) {
        String lower = password.toLowerCase();

        if (email != null) {
            int at = email.indexOf('@');
            String localPart = (at > 0 ? email.substring(0, at) : email).toLowerCase();
            if (localPart.length() >= MIN_TOKEN_LENGTH && lower.contains(localPart)) {
                throw new IllegalArgumentException("비밀번호에 이메일 아이디를 포함할 수 없습니다.");
            }
        }

        if (phone != null) {
            String digits = phone.replaceAll("\\D", "");
            // 뒤 4자리, 가운데 4자리처럼 사람들이 흔히 쓰는 조각을 차단
            for (int i = 0; i + MIN_TOKEN_LENGTH <= digits.length(); i++) {
                String chunk = digits.substring(i, i + MIN_TOKEN_LENGTH);
                if (lower.contains(chunk)) {
                    throw new IllegalArgumentException("비밀번호에 전화번호를 포함할 수 없습니다.");
                }
            }
        }
    }
}