package com.tadaktadak.eunggeubi.domain.user.dto;

// 소셜 전용 계정은 비밀번호가 없어 생략할 수 있다.
public record WithdrawRequest(
        String password
) {
}
