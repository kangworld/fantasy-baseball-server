package com.kangworld.fantasybaseball.roster.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RosterVersionConflictException extends RuntimeException {

    public RosterVersionConflictException() {
        super("로스터가 이미 수정되었습니다. 최신 로스터를 다시 조회해 주세요.");
    }
}
