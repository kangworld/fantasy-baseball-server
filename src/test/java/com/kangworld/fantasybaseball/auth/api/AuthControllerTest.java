package com.kangworld.fantasybaseball.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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

import com.kangworld.fantasybaseball.auth.token.RefreshTokenStore;
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
        String requestBody = """
                {
                  "memberId": 7,
                  "refreshToken": "token-id.secret-value"
                }
                """;

        // when then
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(refreshTokenStore).delete(7L, "token-id");
    }

    private void insertMember(String email, String rawPassword, String displayName, MemberStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 11, 0, 0);

        memberDao.insert(Member.builder()
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
