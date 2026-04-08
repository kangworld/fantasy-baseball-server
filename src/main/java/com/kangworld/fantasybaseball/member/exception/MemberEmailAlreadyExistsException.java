package com.kangworld.fantasybaseball.member.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MemberEmailAlreadyExistsException extends RuntimeException {

    public MemberEmailAlreadyExistsException() {
        super("이미 가입된 이메일입니다.");
    }

}
