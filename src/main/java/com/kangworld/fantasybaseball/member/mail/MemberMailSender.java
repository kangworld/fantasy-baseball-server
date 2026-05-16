package com.kangworld.fantasybaseball.member.mail;

public interface MemberMailSender {

    void sendSignupVerificationCode(String email, String code);

}
