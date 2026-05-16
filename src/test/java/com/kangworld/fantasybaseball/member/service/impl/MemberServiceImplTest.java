package com.kangworld.fantasybaseball.member.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.exception.MemberEmailAlreadyExistsException;
import com.kangworld.fantasybaseball.member.exception.SignupEmailVerificationRequiredException;
import com.kangworld.fantasybaseball.member.mail.MemberMailSender;
import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.service.MemberService;
import com.kangworld.fantasybaseball.member.service.SignupEmailVerificationService;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupRequest;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupResponse;
import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationConfirmRequest;
import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationSendRequest;
import com.kangworld.rediscore.lock.RedisLock;
import com.kangworld.rediscore.lock.RedisLockManager;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "delete from signup_email_verification",
        "delete from member"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MemberServiceImplTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Autowired
    private MemberService memberService;

    @Autowired
    private SignupEmailVerificationService signupEmailVerificationService;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MemberMailSender memberMailSender;

    @MockitoBean
    private RedisLockManager redisLockManager;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2026-05-14T03:00:00Z"));
        given(clock.getZone()).willReturn(SEOUL_ZONE_ID);
        given(redisLockManager.acquire(eq("signup-email-verification"), anyLong(), any()))
                .willReturn(new RedisLock("fantasy-baseball::signup-email-verification", "token"));
    }

    @Test
    void signup은_비밀번호를_해시로_저장한다() {
        // given
        String verificationCode = issueAndConfirmCode("service@example.com");
        MemberSignupRequest request = buildRequest("service@example.com", verificationCode, "plain-password", "cleanup01");

        // when
        MemberSignupResponse response = memberService.signup(request);
        Member savedMember = memberDao.findByMemberId(response.getMemberId()).orElseThrow();

        // then
        assertThat(savedMember.getPasswordHash()).isNotEqualTo("plain-password");
        assertThat(passwordEncoder.matches("plain-password", savedMember.getPasswordHash())).isTrue();
    }

    @Test
    void signup은_중복_이메일이면_예외를_던진다() {
        // given
        String verificationCode = issueAndConfirmCode("dup-service@example.com");
        MemberSignupRequest request = buildRequest("dup-service@example.com", verificationCode, "plain-password", "cleanup01");
        memberService.signup(request);

        // when then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(MemberEmailAlreadyExistsException.class);
    }

    @Test
    void signup은_인증번호가_없으면_예외를_던진다() {
        // given
        MemberSignupRequest request = buildRequest("required@example.com", "123456", "plain-password", "cleanup01");

        // when then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(SignupEmailVerificationRequiredException.class);
    }

    @Test
    void signup_emailVerification은_재전송하면_이전_인증번호를_만료한다() {
        // given
        String email = "resend@example.com";
        signupEmailVerificationService.send(new SignupEmailVerificationSendRequest(email));
        signupEmailVerificationService.send(new SignupEmailVerificationSendRequest(email));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberMailSender, times(2)).sendSignupVerificationCode(eq(email), codeCaptor.capture());
        String firstCode = codeCaptor.getAllValues().get(0);
        String secondCode = codeCaptor.getAllValues().get(1);

        // when then
        assertThatThrownBy(() -> signupEmailVerificationService.confirm(new SignupEmailVerificationConfirmRequest(email, firstCode)))
                .isInstanceOf(SignupEmailVerificationRequiredException.class);

        signupEmailVerificationService.confirm(new SignupEmailVerificationConfirmRequest(email, secondCode));
    }

    private String issueAndConfirmCode(String email) {
        signupEmailVerificationService.send(new SignupEmailVerificationSendRequest(email));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberMailSender).sendSignupVerificationCode(eq(email), codeCaptor.capture());
        String verificationCode = codeCaptor.getValue();

        signupEmailVerificationService.confirm(new SignupEmailVerificationConfirmRequest(email, verificationCode));

        return verificationCode;
    }

    private MemberSignupRequest buildRequest(String email, String verificationCode, String password, String displayName) {
        return new MemberSignupRequest(email, verificationCode, password, displayName);
    }

}
