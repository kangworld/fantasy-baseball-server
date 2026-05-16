package com.kangworld.fantasybaseball.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.auth.exception.LoginFailedException;
import com.kangworld.fantasybaseball.auth.exception.RefreshTokenExpiredException;
import com.kangworld.fantasybaseball.auth.service.AuthService;
import com.kangworld.fantasybaseball.auth.service.dto.LoginRequest;
import com.kangworld.fantasybaseball.auth.service.dto.LoginResponse;
import com.kangworld.fantasybaseball.auth.service.dto.LogoutRequest;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenRequest;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenResponse;
import com.kangworld.fantasybaseball.auth.token.RefreshTokenFactory;
import com.kangworld.fantasybaseball.auth.token.RefreshTokenStore;
import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.model.MemberRole;
import com.kangworld.fantasybaseball.member.model.MemberStatus;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from member", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthServiceImplTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenFactory refreshTokenFactory;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    void login은_토큰을_발급하고_refreshToken을_저장한다() {
        // given
        doNothing().when(refreshTokenStore).save(any());
        insertMember("service-login@example.com", "plain-password", "cleanup01", MemberStatus.OK);
        LoginRequest request = new LoginRequest("service-login@example.com", "plain-password");

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).contains(".");
        assertThat(response.getMember().getEmail()).isEqualTo("service-login@example.com");
        verify(refreshTokenStore).save(any());
    }

    @Test
    void login은_비밀번호가_틀리면_예외를_던진다() {
        // given
        doNothing().when(refreshTokenStore).save(any());
        insertMember("wrong-password-service@example.com", "plain-password", "cleanup01", MemberStatus.OK);
        LoginRequest request = new LoginRequest("wrong-password-service@example.com", "wrong-password");

        // when then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    void login은_DEL_회원이면_예외를_던진다() {
        // given
        doNothing().when(refreshTokenStore).save(any());
        insertMember("deleted-service@example.com", "plain-password", "cleanup01", MemberStatus.DEL);
        LoginRequest request = new LoginRequest("deleted-service@example.com", "plain-password");

        // when then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    void logout은_refreshToken에_해당하는_세션을_삭제한다() {
        // given
        LogoutRequest request = new LogoutRequest("token-id.secret-value");

        // when
        authService.logout(11L, request);

        // then
        verify(refreshTokenStore).delete(11L, "token-id");
    }

    @Test
    void logout은_refreshToken형식이_잘못되면_삭제를_시도하지_않는다() {
        // given
        LogoutRequest request = new LogoutRequest("invalid-token");

        // when
        authService.logout(11L, request);

        // then
        verify(refreshTokenStore, org.mockito.Mockito.never()).delete(any(), any());
    }

    @Test
    void refreshAccessToken은_refreshToken이_유효하면_accessToken을_재발급한다() {
        // given
        long memberId = insertMember("refresh-service@example.com", "plain-password", "cleanup01", MemberStatus.OK);
        doReturn(java.util.Optional.of(refreshTokenFactory.hash("secret-value")))
                .when(refreshTokenStore).findSecretHash(eq(memberId), eq("token-id"));
        RefreshAccessTokenRequest request = new RefreshAccessTokenRequest(memberId, "token-id.secret-value");

        // when
        RefreshAccessTokenResponse response = authService.refreshAccessToken(request);

        // then
        assertThat(response.getAccessToken()).isNotBlank();
    }

    @Test
    void refreshAccessToken은_refreshToken이_없으면_재로그인을_유도한다() {
        // given
        long memberId = insertMember("refresh-expired-service@example.com", "plain-password", "cleanup01", MemberStatus.OK);
        RefreshAccessTokenRequest request = new RefreshAccessTokenRequest(memberId, "token-id.secret-value");

        // when then
        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(RefreshTokenExpiredException.class);
    }

    @Test
    void refreshAccessToken은_secret이_일치하지_않으면_재로그인을_유도한다() {
        // given
        long memberId = insertMember("refresh-mismatch-service@example.com", "plain-password", "cleanup01", MemberStatus.OK);
        doReturn(java.util.Optional.of("different-secret-hash"))
                .when(refreshTokenStore).findSecretHash(eq(memberId), eq("token-id"));
        RefreshAccessTokenRequest request = new RefreshAccessTokenRequest(memberId, "token-id.secret-value");

        // when then
        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(RefreshTokenExpiredException.class);
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

}
