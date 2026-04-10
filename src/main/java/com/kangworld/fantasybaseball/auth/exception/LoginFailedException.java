package com.kangworld.fantasybaseball.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class LoginFailedException extends RuntimeException {

    public LoginFailedException() {
        super("로그인 정보가 올바르지 않습니다.");
    }

}
