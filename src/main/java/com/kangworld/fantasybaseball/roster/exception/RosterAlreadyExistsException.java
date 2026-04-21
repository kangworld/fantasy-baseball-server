package com.kangworld.fantasybaseball.roster.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RosterAlreadyExistsException extends RuntimeException {

    public RosterAlreadyExistsException() {
        super("이미 제출된 로스터가 있습니다.");
    }

}
