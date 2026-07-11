package com.kangworld.fantasybaseball.auth.token;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.auth.config.AuthSecurityProperties;
import com.kangworld.fantasybaseball.auth.security.AuthenticatedMember;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtAccessTokenParser {

    private final SecretKey secretKey;

    public JwtAccessTokenParser(AuthSecurityProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public Optional<AuthenticatedMember> parse(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            return Optional.of(new AuthenticatedMember(
                    Long.valueOf(claims.getSubject()),
                    claims.get("role", String.class)
            ));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

}
