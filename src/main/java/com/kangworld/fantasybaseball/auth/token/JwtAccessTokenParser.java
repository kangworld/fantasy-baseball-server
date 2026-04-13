package com.kangworld.fantasybaseball.auth.token;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.auth.security.AuthenticatedMember;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtAccessTokenParser {

    private final SecretKey secretKey;

    public JwtAccessTokenParser(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<AuthenticatedMember> parse(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            return Optional.of(AuthenticatedMember.builder()
                    .memberId(Long.valueOf(claims.getSubject()))
                    .role(claims.get("role", String.class))
                    .build());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

}
