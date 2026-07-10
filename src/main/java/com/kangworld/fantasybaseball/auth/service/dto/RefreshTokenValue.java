package com.kangworld.fantasybaseball.auth.service.dto;

import java.time.Duration;

public record RefreshTokenValue(
        Long userId,
        String tokenId,
        String rawToken,
        String secretHash,
        Duration ttl
) {
}
