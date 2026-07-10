package com.kangworld.fantasybaseball.auth.api;

import static com.kangworld.fantasybaseball.support.AuthTestCookies.refreshTokenCookie;
import static com.kangworld.fantasybaseball.support.AuthTestCookies.trustedOrigin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.kangworld.fantasybaseball.auth.cookie.AuthCookieFactory;
import com.kangworld.fantasybaseball.auth.token.RefreshTokenFactory;
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

    @Autowired
    private RefreshTokenFactory refreshTokenFactory;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    void login은_토큰을_세션쿠키로_발급하고_body에는_회원정보만_반환한다() throws Exception {
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
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.member.memberId").isNumber())
                .andExpect(jsonPath("$.data.member.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.member.displayName").value("slugger01"))
                .andReturn();

        List<String> cookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertSessionCookie(cookieHeaders, AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME, "/");
        assertSessionCookie(cookieHeaders, AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, "/api/auth");
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인 정보가 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인 정보가 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void logout은_refreshToken에_해당하는_세션을_삭제하고_쿠키를_만료시킨다() throws Exception {
        // given
        long memberId = insertMember("logout@example.com", "plain-password", "slugger01", MemberStatus.OK);
        doReturn(java.util.Optional.of(refreshTokenFactory.hash("secret-value")))
                .when(refreshTokenStore).findSecretHash(memberId, "token-id");

        // when then
        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshTokenCookie(memberId + ".token-id.secret-value")).with(trustedOrigin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        verify(refreshTokenStore).delete(memberId, "token-id");
        List<String> cookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertExpiredCookie(cookieHeaders, AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME, "/");
        assertExpiredCookie(cookieHeaders, AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, "/api/auth");
    }

    @Test
    void logout은_refreshToken이_없어도_쿠키를_만료시킨다() throws Exception {
        // when then
        MvcResult result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        verify(refreshTokenStore, never()).delete(any(), any());
        List<String> cookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertExpiredCookie(cookieHeaders, AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME, "/");
        assertExpiredCookie(cookieHeaders, AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, "/api/auth");
    }

    @Test
    void logout은_허용되지_않은_referer이면_403을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshTokenCookie("11.token-id.secret-value"))
                        .header(HttpHeaders.REFERER, "https://evil.example.com/logout"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("허용되지 않은 요청 출처입니다."));

        verify(refreshTokenStore, never()).delete(any(), any());
    }

    @Test
    void refresh는_refreshToken이_유효하면_두_토큰을_세션쿠키로_재발급한다() throws Exception {
        // given
        long memberId = insertMember("refresh@example.com", "plain-password", "slugger01", MemberStatus.OK);
        doNothing().when(refreshTokenStore).save(any());
        doReturn(java.util.Optional.of(refreshTokenFactory.hash("secret-value")))
                .when(refreshTokenStore).findSecretHash(eq(memberId), eq("token-id"));

        // when then
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshTokenCookie(memberId + ".token-id.secret-value")).with(trustedOrigin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        verify(refreshTokenStore).save(any());
        verify(refreshTokenStore).delete(memberId, "token-id");
        List<String> cookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertSessionCookie(cookieHeaders, AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME, "/");
        assertSessionCookie(cookieHeaders, AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, "/api/auth");
    }

    @Test
    void refresh는_refreshToken이_만료되면_401을_반환한다() throws Exception {
        // given
        long memberId = insertMember("refresh-expired@example.com", "plain-password", "slugger01", MemberStatus.OK);

        // when then
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshTokenCookie(memberId + ".token-id.secret-value")).with(trustedOrigin()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void refresh는_refreshToken_쿠키가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void refresh는_허용되지_않은_referer이면_403을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshTokenCookie("11.token-id.secret-value"))
                        .header(HttpHeaders.REFERER, "https://evil.example.com/refresh"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("허용되지 않은 요청 출처입니다."));

        verify(refreshTokenStore, never()).findSecretHash(any(), any());
    }

    private long insertMember(String email, String rawPassword, String displayName, MemberStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 11, 0, 0);

        return memberDao.insert(Member.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName(displayName)
                .role(MemberRole.USER)
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void assertSessionCookie(List<String> cookieHeaders, String name, String path) {
        String cookieHeader = findCookieHeader(cookieHeaders, name);

        assertThat(cookieHeader)
                .contains("Path=" + path, "HttpOnly", "SameSite=Lax")
                .doesNotContain("Max-Age", "Expires");
    }

    private void assertExpiredCookie(List<String> cookieHeaders, String name, String path) {
        String cookieHeader = findCookieHeader(cookieHeaders, name);

        assertThat(cookieHeader)
                .contains("Path=" + path, "HttpOnly", "SameSite=Lax", "Max-Age=0");
    }

    private String findCookieHeader(List<String> cookieHeaders, String name) {
        return cookieHeaders.stream()
                .filter(cookieHeader -> cookieHeader.startsWith(name + "="))
                .findFirst()
                .orElseThrow();
    }

}
