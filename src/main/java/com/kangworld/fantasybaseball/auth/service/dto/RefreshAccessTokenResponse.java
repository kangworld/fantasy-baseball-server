package com.kangworld.fantasybaseball.auth.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshAccessTokenResponse {

    private String accessToken;

}
