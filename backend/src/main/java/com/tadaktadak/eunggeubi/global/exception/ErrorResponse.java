package com.tadaktadak.eunggeubi.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

// code: 프론트가 상태코드만으로 구분하기 애매한 에러를 식별할 때만 채운다(예: GUEST_LIMIT). 없으면 응답에서 빠진다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, String code) {

    public ErrorResponse(String message) {
        this(message, null);
    }
}
