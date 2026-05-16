package com.kangworld.fantasybaseball.member.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class SignupEmailVerificationSendFailedException extends RuntimeException {

    public SignupEmailVerificationSendFailedException(Throwable cause) {
        super("인증번호 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", cause);
    }

}
