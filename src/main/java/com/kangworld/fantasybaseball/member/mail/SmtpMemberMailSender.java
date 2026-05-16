package com.kangworld.fantasybaseball.member.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.member.exception.SignupEmailVerificationSendFailedException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SmtpMemberMailSender implements MemberMailSender {

    private final JavaMailSender javaMailSender;

    @Value("${member.mail.from}")
    private String from;

    @Value("${member.signup.email-verification.code-expiration-minutes}")
    private long codeExpirationMinutes;

    @Override
    public void sendSignupVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[fantasy-baseball] 회원가입 인증번호");
        message.setText("""
                인증번호: %s

                인증번호 유효 시간은 %d분입니다. 재전송 시 이전 인증번호는 만료됩니다.
                요청하지 않은 인증번호라면 이 메일을 무시해주세요.
                """.formatted(code, codeExpirationMinutes));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new SignupEmailVerificationSendFailedException(exception);
        }
    }

}
