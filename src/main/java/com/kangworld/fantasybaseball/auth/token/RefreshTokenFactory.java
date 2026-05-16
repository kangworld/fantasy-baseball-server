package com.kangworld.fantasybaseball.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenParts;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenValue;

@Component
public class RefreshTokenFactory {

    private final long refreshTokenExpirationSeconds;

    public RefreshTokenFactory(@Value("${security.jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds) {
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public RefreshTokenValue create(Long userId) {
        String tokenId = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        // tokenId는 Redis 조회 키, secret은 실제 소유 증명값으로 분리한다.
        // Redis에는 원본 토큰을 저장하지 않고 secret hash만 저장한다.
        String rawToken = tokenId + "." + secret;

        return RefreshTokenValue.builder()
                .userId(userId)
                .tokenId(tokenId)
                .rawToken(rawToken)
                .secretHash(hash(secret))
                .ttl(Duration.ofSeconds(refreshTokenExpirationSeconds))
                .build();
    }

    public RefreshTokenParts parse(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String[] parts = rawToken.split("\\.", 2);

        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }

        return RefreshTokenParts.builder()
                .tokenId(parts[0])
                .secret(parts[1])
                .build();
    }

    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

}
