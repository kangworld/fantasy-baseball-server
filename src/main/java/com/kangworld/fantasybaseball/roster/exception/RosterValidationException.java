package com.kangworld.fantasybaseball.roster.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RosterValidationException extends RuntimeException {

    public RosterValidationException(String message) {
        super(message);
    }

}
