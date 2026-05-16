package com.kangworld.fantasybaseball.member.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SignupEmailVerificationRequiredException extends RuntimeException {

    public SignupEmailVerificationRequiredException() {
        super("이메일 인증이 필요합니다.");
    }

}
