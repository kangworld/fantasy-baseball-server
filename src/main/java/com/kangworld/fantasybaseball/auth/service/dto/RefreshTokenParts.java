package com.kangworld.fantasybaseball.auth.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenParts {

    private Long userId;
    private String tokenId;
    private String secret;

}
