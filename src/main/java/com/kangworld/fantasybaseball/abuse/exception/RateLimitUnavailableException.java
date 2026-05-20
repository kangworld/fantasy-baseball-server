package com.kangworld.fantasybaseball.abuse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class RateLimitUnavailableException extends RuntimeException {

    public RateLimitUnavailableException(String message) {
        super(message);
    }

    public RateLimitUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}


