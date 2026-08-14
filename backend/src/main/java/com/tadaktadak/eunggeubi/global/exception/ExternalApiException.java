package com.tadaktadak.eunggeubi.global.exception;

// 외부 API(공공데이터 등) 호출 실패 전용 예외
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}