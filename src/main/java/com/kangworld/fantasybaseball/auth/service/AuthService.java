package com.kangworld.fantasybaseball.auth.service;

import com.kangworld.fantasybaseball.auth.service.dto.LoginRequest;
import com.kangworld.fantasybaseball.auth.service.dto.LoginResponse;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    RefreshAccessTokenResponse refreshAccessToken(String refreshToken);

    void logout(String refreshToken);

}
