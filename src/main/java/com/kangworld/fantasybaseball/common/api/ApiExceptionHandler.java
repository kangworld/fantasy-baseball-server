package com.kangworld.fantasybaseball.common.api;

import com.kangworld.fantasybaseball.abuse.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
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

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(RateLimitExceededException exception) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException exception, HttpServletRequest request) {
        // 도메인 예외는 각 예외 클래스의 @ResponseStatus를 API 응답 상태로 사용한다.
        MergedAnnotation<ResponseStatus> responseStatus = MergedAnnotations.from(exception.getClass())
                .get(ResponseStatus.class);
        if (!responseStatus.isPresent()) {
            log.error("Unhandled API exception. method={}, uri={}", request.getMethod(), request.getRequestURI(), exception);

            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        }

        HttpStatus status = responseStatus.synthesize().code();
        String reason = responseStatus.synthesize().reason();
        String message = StringUtils.hasText(reason) ? reason : exception.getMessage();

        String risk = status.is5xxServerError() ? "HIGH" : "LOW";
        log.error("[{}] Handled API exception. method={}, uri={}, status={}, message={}",
                risk, request.getMethod(), request.getRequestURI(), status.value(), message, exception);

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
