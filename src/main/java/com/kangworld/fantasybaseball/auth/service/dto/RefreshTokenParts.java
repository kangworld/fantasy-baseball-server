package com.kangworld.fantasybaseball.auth.service.dto;

public record RefreshTokenParts(Long userId, String tokenId, String secret) {
}
