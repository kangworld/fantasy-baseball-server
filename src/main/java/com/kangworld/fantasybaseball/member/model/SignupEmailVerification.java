package com.kangworld.fantasybaseball.member.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupEmailVerification {

    private Long signupEmailVerificationId;
    private String email;
    private String codeHash;
    private LocalDateTime codeExpiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime verifiedExpiresAt;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
