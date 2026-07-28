package com.tadaktadak.eunggeubi.domain.auth.dto;

public record LoginResponse(
        Long userId,
        String accessToken,
        String refreshToken,
        String tokenType     // "Bearer"
) {
}
