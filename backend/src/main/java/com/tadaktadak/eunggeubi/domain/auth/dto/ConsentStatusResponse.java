package com.tadaktadak.eunggeubi.domain.auth.dto;

public record ConsentStatusResponse(
        boolean confirmed   // true면 동의 완료 → 로그인 가능
) {
}