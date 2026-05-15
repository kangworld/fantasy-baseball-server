package com.kangworld.fantasybaseball.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, getValidationMessage(exception));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException exception) {
        // 도메인 예외는 각 예외 클래스의 @ResponseStatus를 API 응답 상태로 사용한다.
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class);
        if (responseStatus == null) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }

        HttpStatus status = responseStatus.code();
        String message = StringUtils.hasText(responseStatus.reason())
                ? responseStatus.reason()
                : exception.getMessage();
        return buildResponse(status, message);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.of(status, message, null));
    }

    private String getValidationMessage(MethodArgumentNotValidException exception) {
        // RequestBody 검증 실패는 클라이언트가 바로 표시할 수 있도록 첫 번째 필드 메시지를 대표 메시지로 쓴다.
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError == null || !StringUtils.hasText(fieldError.getDefaultMessage()))
            return "요청값이 올바르지 않습니다.";

        return fieldError.getDefaultMessage();
    }

}
