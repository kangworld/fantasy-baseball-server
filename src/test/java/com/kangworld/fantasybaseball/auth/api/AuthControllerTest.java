package com.kangworld.fantasybaseball.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.kangworld.fantasybaseball.auth.token.RefreshTokenFactory;
import com.kangworld.fantasybaseball.auth.token.RefreshTokenStore;
import com.kangworld.fantasybaseball.auth.token.JwtAccessTokenFactory;
import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.model.MemberRole;
import com.kangworld.fantasybaseball.member.model.MemberStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = "delete from member", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenFactory refreshTokenFactory;

    @Autowired
    private JwtAccessTokenFactory jwtAccessTokenFactory;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    void login은_토큰과_회원정보를_반환한다() throws Exception {
        // given
        doNothing().when(refreshTokenStore).save(any());
        insertMember("login@example.com", "plain-password", "slugger01", MemberStatus.OK);

        String requestBody = """
                {
                  "email": "login@example.com",
                  "password": "plain-password"
                }
                """;

        // when then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.member.memberId").isNumber())
                .andExpect(jsonPath("$.data.member.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.member.displayName").value("slugger01"));
    }

    @Test
    void login은_비밀번호가_틀리면_401을_반환한다() throws Exception {
        // given
        doNothing().when(refreshTokenStore).save(any());
        insertMember("wrong-password@example.com", "plain-password", "slugger01", MemberStatus.OK);

        String requestBody = """
                {
                  "email": "wrong-password@example.com",
                  "password": "invalid-password"
                }
                """;

        // when then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login은_DEL_회원이면_401을_반환한다() throws Exception {
        // given
        doNothing().when(refreshTokenStore).save(any());
        insertMember("deleted@example.com", "plain-password", "slugger01", MemberStatus.DEL);

        String requestBody = """
                {
                  "email": "deleted@example.com",
                  "password": "plain-password"
                }
                """;

        // when then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout은_refreshToken에_해당하는_세션을_삭제한다() throws Exception {
        // given
        long memberId = insertMember("logout@example.com", "plain-password", "slugger01", MemberStatus.OK);
        String accessToken = jwtAccessTokenFactory.create(memberId, MemberRole.USER.name());
        String requestBody = """
                {
                  "refreshToken": "token-id.secret-value"
                }
                """;

        // when then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(refreshTokenStore).delete(memberId, "token-id");
    }

    @Test
    void logout은_accessToken이_없으면_401을_반환한다() throws Exception {
        // given
        String requestBody = """
                {
                  "refreshToken": "token-id.secret-value"
                }
                """;

        // when then
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh는_refreshToken이_유효하면_accessToken을_재발급한다() throws Exception {
        // given
        long memberId = insertMember("refresh@example.com", "plain-password", "slugger01", MemberStatus.OK);
        doReturn(java.util.Optional.of(refreshTokenFactory.hash("secret-value")))
                .when(refreshTokenStore).findSecretHash(eq(memberId), eq("token-id"));

        String requestBody = """
                {
                  "memberId": %d,
                  "refreshToken": "token-id.secret-value"
                }
                """.formatted(memberId);

        // when then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    void refresh는_refreshToken이_만료되면_401을_반환한다() throws Exception {
        // given
        long memberId = insertMember("refresh-expired@example.com", "plain-password", "slugger01", MemberStatus.OK);
        String requestBody = """
                {
                  "memberId": %d,
                  "refreshToken": "token-id.secret-value"
                }
                """.formatted(memberId);

        // when then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    private long insertMember(String email, String rawPassword, String displayName, MemberStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 11, 0, 0);

        return memberDao.insert(Member.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName(displayName)
                .role(MemberRole.USER)
                .status(status)
                .emailVerified(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

}
