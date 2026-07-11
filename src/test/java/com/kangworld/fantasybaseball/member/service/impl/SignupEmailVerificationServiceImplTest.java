package com.kangworld.fantasybaseball.member.service.impl;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.member.config.MemberProperties;
import com.kangworld.fantasybaseball.member.dao.SignupEmailVerificationDao;
import com.kangworld.fantasybaseball.member.mail.MemberMailSender;
import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationSendRequest;

@ExtendWith(MockitoExtension.class)
class SignupEmailVerificationServiceImplTest {

    @Mock
    private SignupEmailVerificationDao signupEmailVerificationDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberMailSender memberMailSender;

    @Mock
    private FantasyTime fantasyTime;

    @Mock
    private SignupEmailVerificationPersistenceService persistenceService;

    private SignupEmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        MemberProperties properties = new MemberProperties(
                new MemberProperties.Mail("from@example.com"),
                new MemberProperties.Signup(new MemberProperties.EmailVerification(5, 10)));
        service = new SignupEmailVerificationServiceImpl(
                signupEmailVerificationDao,
                passwordEncoder,
                memberMailSender,
                fantasyTime,
                persistenceService,
                properties);
    }

    @Test
    void send는_인증번호를_커밋한_뒤_메일을_발송한다() {
        // given
        String email = "member@example.com";
        LocalDateTime now = LocalDateTime.of(2026, 7, 11, 13, 25);
        given(fantasyTime.getDateTime()).willReturn(now);
        given(passwordEncoder.encode(anyString())).willReturn("code-hash");

        // when
        service.send(new SignupEmailVerificationSendRequest(email));

        // then
        InOrder inOrder = inOrder(persistenceService, memberMailSender);
        inOrder.verify(persistenceService).saveCode(
                eq(email),
                eq("code-hash"),
                eq(now.plusMinutes(5)),
                eq(now));
        inOrder.verify(memberMailSender).sendSignupVerificationCode(eq(email), anyString());
    }

}
