package com.kangworld.fantasybaseball.member.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.kangworld.fantasybaseball.member.mail.MemberMailSender;
import com.kangworld.rediscore.lock.RedisLock;
import com.kangworld.rediscore.lock.RedisLockManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
        "delete from signup_email_verification",
        "delete from member"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MemberControllerTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Autowired
    private MockMvc mockMvc;

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
    void signup_emailVerification은_인증번호를_발송한다() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "signup-code@example.com"
                }
                """;

        // when then
        mockMvc.perform(post("/api/members/signup/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("인증번호를 발송했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(memberMailSender).sendSignupVerificationCode(eq("signup-code@example.com"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void signup은_회원을_생성하고_기본_정보를_반환한다() throws Exception {
        // given
        String verificationCode = issueAndConfirmCode("signup@example.com");
        String requestBody = """
                {
                  "email": "signup@example.com",
                  "verificationCode": "%s",
                  "password": "plain-password",
                  "displayName": "slugger01"
                }
                """.formatted(verificationCode);

        // when then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.memberId").isNumber())
                .andExpect(jsonPath("$.data.email").value("signup@example.com"))
                .andExpect(jsonPath("$.data.displayName").value("slugger01"));
    }

    @Test
    void signup은_중복_이메일이면_409를_반환한다() throws Exception {
        // given
        String firstVerificationCode = issueAndConfirmCode("dup@example.com");
        String firstRequestBody = """
                {
                  "email": "dup@example.com",
                  "verificationCode": "%s",
                  "password": "plain-password",
                  "displayName": "slugger01"
                }
                """.formatted(firstVerificationCode);

        String secondRequestBody = """
                {
                  "email": "dup@example.com",
                  "verificationCode": "123456",
                  "password": "another-password",
                  "displayName": "slugger02"
                }
                """;

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequestBody))
                .andExpect(status().isCreated());

        // when then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRequestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void signup은_잘못된_displayName이면_400을_반환한다() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "invalid@example.com",
                  "verificationCode": "123456",
                  "password": "plain-password",
                  "displayName": "slugger-01"
                }
                """;

        // when then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void signup은_이메일_인증이_없으면_400을_반환한다() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "not-verified@example.com",
                  "verificationCode": "123456",
                  "password": "plain-password",
                  "displayName": "slugger01"
                }
                """;

        // when then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 인증이 필요합니다."));
    }

    @Test
    void signup은_인증완료_후에도_최종가입_인증번호가_틀리면_400을_반환한다() throws Exception {
        // given
        issueAndConfirmCode("wrong-final-code@example.com");
        String requestBody = """
                {
                  "email": "wrong-final-code@example.com",
                  "verificationCode": "000000",
                  "password": "plain-password",
                  "displayName": "slugger01"
                }
                """;

        // when then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 인증이 필요합니다."));
    }

    private String issueAndConfirmCode(String email) throws Exception {
        mockMvc.perform(post("/api/members/signup/email-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s"
                        }
                        """.formatted(email)));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberMailSender).sendSignupVerificationCode(eq(email), codeCaptor.capture());
        String verificationCode = codeCaptor.getValue();

        mockMvc.perform(post("/api/members/signup/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "code": "%s"
                                }
                                """.formatted(email, verificationCode)))
                .andExpect(status().isOk());

        return verificationCode;
    }

}
