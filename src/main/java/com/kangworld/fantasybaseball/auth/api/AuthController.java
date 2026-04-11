package com.kangworld.fantasybaseball.auth.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RefreshAccessTokenResponse> refresh(@Valid @RequestBody RefreshAccessTokenRequest request) {
        return ApiResponse.ok(authService.refreshAccessToken(request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.of(HttpStatus.OK, "로그아웃이 완료되었습니다.", null);
    }

}
