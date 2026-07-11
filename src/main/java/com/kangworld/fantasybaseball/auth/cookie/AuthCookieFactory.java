package com.kangworld.fantasybaseball.auth.cookie;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.auth.config.AuthSecurityProperties;

@Component
public class AuthCookieFactory {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private static final String ACCESS_TOKEN_PATH = "/";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";
    private static final String SAME_SITE = "Lax";

    private final boolean secure;

    public AuthCookieFactory(AuthSecurityProperties properties) {
        this.secure = properties.authCookie().secure();
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return sessionCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken, ACCESS_TOKEN_PATH).build();
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return sessionCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, REFRESH_TOKEN_PATH).build();
    }

    public ResponseCookie expireAccessTokenCookie() {
        return sessionCookie(ACCESS_TOKEN_COOKIE_NAME, "", ACCESS_TOKEN_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return sessionCookie(REFRESH_TOKEN_COOKIE_NAME, "", REFRESH_TOKEN_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder sessionCookie(String name, String value, String path) {
        // Max-Age와 Expires를 지정하지 않아 브라우저 종료 시 제거되는 세션 쿠키로 발급한다.
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(path);
    }

}
