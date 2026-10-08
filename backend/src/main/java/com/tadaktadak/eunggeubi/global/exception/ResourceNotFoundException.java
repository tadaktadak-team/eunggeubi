package com.tadaktadak.eunggeubi.global.exception;

/**
 * 요청한 대상이 없을 때 던진다 -> 404.
 *
 * 입력 형식이 틀린 400 과 구분하려고 따로 둔다. 메시지에 요청 값을 그대로 넣지 않는다
 * (잘못된 값이 응답과 로그에 되돌아오지 않게).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
