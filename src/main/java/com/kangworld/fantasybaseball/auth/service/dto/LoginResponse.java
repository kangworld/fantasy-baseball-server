package com.kangworld.fantasybaseball.auth.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    @JsonIgnore
    private String accessToken;

    @JsonIgnore
    private String refreshToken;
    private LoginMember member;

}
