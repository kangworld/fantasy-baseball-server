package com.kangworld.fantasybaseball.support;

import com.kangworld.fantasybaseball.auth.cookie.AuthCookieFactory;

import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class AuthTestCookies {

    private AuthTestCookies() {
    }

    public static Cookie accessTokenCookie(String accessToken) {
        return new Cookie(AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME, accessToken);
    }

    public static Cookie refreshTokenCookie(String refreshToken) {
        return new Cookie(AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, refreshToken);
    }

    public static RequestPostProcessor trustedOrigin() {
        return request -> {
            request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
            return request;
        };
    }

}
