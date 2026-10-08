package com.tadaktadak.eunggeubi.global.exception;

import com.tadaktadak.eunggeubi.domain.ai_consultations.service.GuestLimitExceededException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 비즈니스 규칙 위반 (중복 이메일, 로그인 실패 등) -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    // @Valid 검증 실패 -> 400 (부모 메서드를 오버라이드해서 우리 메시지 형식으로)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.isBindingFailure()
                        // "abc" 를 int 로 못 바꾼 경우 등. 스프링의 변환 오류 문장에는 입력값과 내부 타입명이 들어 있어 쓰지 않는다
                        ? invalidFormatMessage(err.getField())
                        : err.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    // 쿼리/경로 값의 타입이 안 맞을 때(예: pageNo=abc) -> 400. 기본 처리는 입력값이 들어간 문장을 돌려준다
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : ex.getPropertyName();
        return ResponseEntity.badRequest().body(new ErrorResponse(invalidFormatMessage(name)));
    }

    private String invalidFormatMessage(String name) {
        return (name == null || name.isBlank()) ? "요청 값의 형식이 올바르지 않습니다." : name + " 값의 형식이 올바르지 않습니다.";
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = ex.getAllErrors().stream()
                .findFirst()
                .map(MessageSourceResolvable::getDefaultMessage)
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    // 요청한 대상이 없음 -> 404 (입력 형식 오류인 400 과 구분)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    // 예상 못 한 모든 예외 -> 500 (내부 정보는 숨기고 일반 문구)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);   // 상세는 로그에만
        return ResponseEntity.status(500).body(new ErrorResponse("서버 오류가 발생했습니다."));
    }

    // 비회원 AI 상담 횟수 초과 -> 403 (프론트가 회원가입 안내로 전환)
    @ExceptionHandler(GuestLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleGuestLimit(GuestLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage(), "GUEST_LIMIT"));
    }

    // 외부 API 호출 실패 -> 502
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternalApi(ExternalApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(e.getMessage()));
    }

    // 소셜 로그인 이메일이 기존 계정과 겹침 -> 409
    @ExceptionHandler(SocialEmailConflictException.class)
    public ResponseEntity<ErrorResponse> handleSocialEmailConflict(SocialEmailConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }
}