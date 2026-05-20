package com.kangworld.fantasybaseball.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.kangworld.fantasybaseball.abuse.exception.RateLimitExceededException;

class ApiExceptionHandlerRateLimitTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleRateLimitExceeded는_간단한_429_응답을_반환한다() {
        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleRateLimitExceeded(new RateLimitExceededException());

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody().getMessage()).isEqualTo("잠시 후 다시 시도해주세요.");
    }

}

