package com.kangworld.fantasybaseball.auth.token;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.auth.config.AuthSecurityProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtAccessTokenFactory {

    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;

    public JwtAccessTokenFactory(AuthSecurityProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = properties.jwt().accessTokenExpirationSeconds();
    }

    public String create(Long userId, String role) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpirationSeconds)))
                .signWith(secretKey)
                .compact();
    }

}
