package com.tadaktadak.eunggeubi.global.util;

public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String digitsOnly(String phone) {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }

    // 로그용: 01012345678 -> 010****5678 (자릿수가 모자라면 전부 가린다)
    public static String mask(String phone) {
        String digits = digitsOnly(phone);
        if (digits == null || digits.length() < 8) {
            return "***";
        }
        return digits.substring(0, 3) + "*".repeat(digits.length() - 7) + digits.substring(digits.length() - 4);
    }
}
