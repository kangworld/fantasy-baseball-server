package com.kangworld.fantasybaseball.member.service;

import java.time.LocalDateTime;

import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationConfirmRequest;
import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationSendRequest;

public interface SignupEmailVerificationService {

    void send(SignupEmailVerificationSendRequest request);

    void confirm(SignupEmailVerificationConfirmRequest request);

    void consumeVerifiedEmail(String email, String verificationCode, LocalDateTime now);

}
