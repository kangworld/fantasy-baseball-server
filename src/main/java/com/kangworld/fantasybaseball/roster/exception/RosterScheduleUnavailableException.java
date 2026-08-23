package com.kangworld.fantasybaseball.roster.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class RosterScheduleUnavailableException extends RuntimeException {

    public RosterScheduleUnavailableException(Throwable cause) {
        super("경기 일정을 확인할 수 없습니다. 잠시 뒤 다시 시도해 주세요.", cause);
    }
}
