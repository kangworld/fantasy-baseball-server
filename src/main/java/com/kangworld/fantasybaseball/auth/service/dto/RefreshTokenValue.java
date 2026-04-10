package com.kangworld.fantasybaseball.auth.service.dto;

import java.time.Duration;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenValue {

    private Long userId;
    private String tokenId;
    private String rawToken;
    private String secretHash;
    private Duration ttl;

}
