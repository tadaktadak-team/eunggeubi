package com.tadaktadak.eunggeubi.domain.user.dto;

//비밀번호 변경 시 기존 세션을 전부 끊음 + 현재 기기가 계속 쓸 새 토큰을 반환(현재 기기에서는 유지)
public record ChangePasswordResponse(
        String accessToken,
        String refreshToken
) {
}