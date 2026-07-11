package com.kangworld.fantasybaseball.member.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.dao.SignupEmailVerificationDao;
import com.kangworld.fantasybaseball.member.exception.SignupEmailVerificationRequiredException;
import com.kangworld.fantasybaseball.member.model.SignupEmailVerification;

@ExtendWith(MockitoExtension.class)
class SignupEmailVerificationPersistenceServiceTest {

    @Mock
    private MemberDao memberDao;

    @Mock
    private SignupEmailVerificationDao signupEmailVerificationDao;

    private SignupEmailVerificationPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new SignupEmailVerificationPersistenceService(memberDao, signupEmailVerificationDao);
    }

    @Test
    void saveCode는_진행중인_인증이_없으면_새로_저장한다() {
        // given
        String email = "member@example.com";
        LocalDateTime now = LocalDateTime.of(2026, 7, 11, 13, 25);
        LocalDateTime expiresAt = now.plusMinutes(5);
        given(memberDao.findByEmail(email)).willReturn(Optional.empty());
        given(signupEmailVerificationDao.findActiveByEmail(email, now)).willReturn(Optional.empty());

        // when
        service.saveCode(email, "code-hash", expiresAt, now);

        // then
        verify(signupEmailVerificationDao).insert(email, "code-hash", expiresAt, now);
    }

    @Test
    void saveCode는_진행중인_인증의_갱신에_실패하면_예외를_발생시킨다() {
        // given
        String email = "member@example.com";
        LocalDateTime now = LocalDateTime.of(2026, 7, 11, 13, 25);
        LocalDateTime expiresAt = now.plusMinutes(5);
        SignupEmailVerification verification = SignupEmailVerification.builder()
                .signupEmailVerificationId(10L)
                .build();
        given(memberDao.findByEmail(email)).willReturn(Optional.empty());
        given(signupEmailVerificationDao.findActiveByEmail(email, now)).willReturn(Optional.of(verification));
        given(signupEmailVerificationDao.updateCode(10L, "code-hash", expiresAt, now)).willReturn(0);

        // when, then
        assertThatThrownBy(() -> service.saveCode(email, "code-hash", expiresAt, now))
                .isInstanceOf(SignupEmailVerificationRequiredException.class);
    }

}
