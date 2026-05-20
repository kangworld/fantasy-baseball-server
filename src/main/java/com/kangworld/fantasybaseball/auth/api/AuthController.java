package com.kangworld.fantasybaseball.auth.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.abuse.AbuseLimit;
import com.kangworld.fantasybaseball.auth.security.AuthenticatedMember;
import com.kangworld.fantasybaseball.auth.service.AuthService;
import com.kangworld.fantasybaseball.auth.service.dto.LoginRequest;
import com.kangworld.fantasybaseball.auth.service.dto.LoginResponse;
import com.kangworld.fantasybaseball.auth.service.dto.LogoutRequest;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenRequest;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenResponse;
import com.kangworld.fantasybaseball.common.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @AbuseLimit(policy = "login-email", key = "#request.email", maxAttempts = 20, window = "5m")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshAccessTokenResponse> refresh(@Valid @RequestBody RefreshAccessTokenRequest request) {
        return ApiResponse.ok(authService.refreshAccessToken(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(authenticatedMember.getMemberId(), request);
        return ApiResponse.of(HttpStatus.OK, "로그아웃이 완료되었습니다.", null);
    }

}
