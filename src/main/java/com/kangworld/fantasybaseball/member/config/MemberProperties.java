package com.kangworld.fantasybaseball.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "member")
public record MemberProperties(
        Mail mail,
        Signup signup
) {

    public record Mail(String from) {
    }

    public record Signup(EmailVerification emailVerification) {
    }

    public record EmailVerification(
            long codeExpirationMinutes,
            long verifiedExpirationMinutes
    ) {
    }

}
