package com.tadaktadak.eunggeubi.global.util;

public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String digitsOnly(String phone) {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }
}
