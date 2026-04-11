package com.kangworld.fantasybaseball.auth.service;

import com.kangworld.fantasybaseball.auth.service.dto.LoginRequest;
import com.kangworld.fantasybaseball.auth.service.dto.LoginResponse;
import com.kangworld.fantasybaseball.auth.service.dto.LogoutRequest;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenRequest;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    RefreshAccessTokenResponse refreshAccessToken(RefreshAccessTokenRequest request);

    void logout(LogoutRequest request);

}
