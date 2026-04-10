package com.kangworld.fantasybaseball.auth.service;

import com.kangworld.fantasybaseball.auth.service.dto.LoginRequest;
import com.kangworld.fantasybaseball.auth.service.dto.LoginResponse;
import com.kangworld.fantasybaseball.auth.service.dto.LogoutRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(LogoutRequest request);

}
