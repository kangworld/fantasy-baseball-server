package com.kangworld.fantasybaseball.member.dao;

import java.time.LocalDateTime;
import java.util.Optional;

import com.kangworld.fantasybaseball.member.model.SignupEmailVerification;

public interface SignupEmailVerificationDao {

    Optional<SignupEmailVerification> findActiveByEmail(String email, LocalDateTime now);

    Optional<SignupEmailVerification> findConfirmableByEmail(String email, LocalDateTime now);

    Optional<SignupEmailVerification> findVerifiedForSignup(String email, LocalDateTime now);

    long insert(String email, String codeHash, LocalDateTime codeExpiresAt, LocalDateTime now);

    int updateCode(long signupEmailVerificationId, String codeHash, LocalDateTime codeExpiresAt, LocalDateTime now);

    int verify(long signupEmailVerificationId, LocalDateTime verifiedAt, LocalDateTime verifiedExpiresAt);

    int consume(long signupEmailVerificationId, LocalDateTime consumedAt);

}
