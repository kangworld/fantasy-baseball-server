package com.kangworld.fantasybaseball.roster.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RosterNotFoundException extends RuntimeException {

    public RosterNotFoundException() {
        super("수정할 로스터가 없습니다.");
    }

}
