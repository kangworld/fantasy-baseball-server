package com.kangworld.fantasybaseball.auth.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.abuse.AbuseLimit;
import com.kangworld.fantasybaseball.auth.cookie.AuthCookieFactory;
import com.kangworld.fantasybaseball.auth.service.AuthService;
import com.kangworld.fantasybaseball.auth.service.dto.LoginRequest;
import com.kangworld.fantasybaseball.auth.service.dto.LoginResponse;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenResponse;
import com.kangworld.fantasybaseball.common.api.ApiResponse;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieFactory authCookieFactory;

    @PostMapping("/login")
    @AbuseLimit(policy = "login-email", key = "#request.email", maxAttempts = 20, window = "5m")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = authService.login(request);
        addAuthCookies(response, loginResponse.getAccessToken(), loginResponse.getRefreshToken());

        return ApiResponse.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ApiResponse<Void> refresh(
            @CookieValue(name = AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        RefreshAccessTokenResponse refreshResponse = authService.refreshAccessToken(refreshToken);
        addAuthCookies(response, refreshResponse.getAccessToken(), refreshResponse.getRefreshToken());

        return ApiResponse.of(HttpStatus.OK, "OK", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.expireAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.expireRefreshTokenCookie().toString());

        return ApiResponse.of(HttpStatus.OK, "로그아웃이 완료되었습니다.", null);
    }

    private void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.createAccessTokenCookie(accessToken).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.createRefreshTokenCookie(refreshToken).toString());
    }

}
