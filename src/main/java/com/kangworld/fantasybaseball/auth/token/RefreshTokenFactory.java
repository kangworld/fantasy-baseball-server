package com.kangworld.fantasybaseball.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.auth.config.AuthSecurityProperties;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenParts;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenValue;

@Component
public class RefreshTokenFactory {

    private final long refreshTokenExpirationSeconds;

    public RefreshTokenFactory(AuthSecurityProperties properties) {
        this.refreshTokenExpirationSeconds = properties.jwt().refreshTokenExpirationSeconds();
    }

    public RefreshTokenValue create(Long userId) {
        String tokenId = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        // memberId는 쿠키만으로 Redis key를 찾기 위한 식별값이고, tokenId와 secret은 조회 키와 소유 증명값으로 분리한다.
        // Redis에는 원본 토큰을 저장하지 않고 secret hash만 저장한다.
        String rawToken = userId + "." + tokenId + "." + secret;

        return new RefreshTokenValue(
                userId,
                tokenId,
                rawToken,
                hash(secret),
                Duration.ofSeconds(refreshTokenExpirationSeconds)
        );
    }

    public RefreshTokenParts parse(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String[] parts = rawToken.split("\\.", 3);

        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            return null;
        }

        try {
            return new RefreshTokenParts(Long.parseLong(parts[0]), parts[1], parts[2]);
        } catch (NumberFormatException exception) {
            return null;
        }
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
