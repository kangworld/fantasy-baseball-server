package com.kangworld.fantasybaseball.auth.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.kangworld.fantasybaseball.auth.config.AuthSecurityProperties;

class AuthCookieFactoryTest {

    private final AuthCookieFactory authCookieFactory = new AuthCookieFactory(new AuthSecurityProperties(
            new AuthSecurityProperties.Jwt("test-secret", 3600, 1209600),
            new AuthSecurityProperties.AuthCookie(true),
            new AuthSecurityProperties.Cors(java.util.List.of("http://localhost:5173"))));

    @Test
    void accessToken은_secure_HttpOnly_Lax_세션쿠키로_발급한다() {
        ResponseCookie cookie = authCookieFactory.createAccessTokenCookie("access-token");

        assertThat(cookie.getName()).isEqualTo(AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("access-token");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge()).isNegative();
    }

    @Test
    void refreshToken은_인증경로의_세션쿠키로_발급한다() {
        ResponseCookie cookie = authCookieFactory.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.getName()).isEqualTo(AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge()).isNegative();
    }

    @Test
    void 만료쿠키는_동일한_경로와_0초_maxAge를_사용한다() {
        ResponseCookie accessCookie = authCookieFactory.expireAccessTokenCookie();
        ResponseCookie refreshCookie = authCookieFactory.expireRefreshTokenCookie();

        assertThat(accessCookie.getPath()).isEqualTo("/");
        assertThat(accessCookie.getMaxAge()).isZero();
        assertThat(refreshCookie.getPath()).isEqualTo("/api/auth");
        assertThat(refreshCookie.getMaxAge()).isZero();
    }

}
