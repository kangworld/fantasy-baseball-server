package com.kangworld.fantasybaseball.abuse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("잠시 후 다시 시도해주세요.");
    }

}


