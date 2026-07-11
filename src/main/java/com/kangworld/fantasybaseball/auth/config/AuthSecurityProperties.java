package com.kangworld.fantasybaseball.auth.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record AuthSecurityProperties(
        Jwt jwt,
        AuthCookie authCookie,
        Cors cors
) {

    public record Jwt(
            String secret,
            long accessTokenExpirationSeconds,
            long refreshTokenExpirationSeconds
    ) {
    }

    public record AuthCookie(boolean secure) {
    }

    public record Cors(List<String> allowedOrigins) {

        public Cors {
            allowedOrigins = allowedOrigins.stream()
                    .map(String::trim)
                    .toList();
        }

    }

}
