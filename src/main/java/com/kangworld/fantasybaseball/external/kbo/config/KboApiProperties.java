package com.kangworld.fantasybaseball.external.kbo.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.kbo")
public record KboApiProperties(
        String baseUrl,
        String imageBaseUrl,
        Duration scheduleCacheTtl,
        Duration recordCacheTtl,
        Duration liveGameStateCacheTtl,
        Duration teamRankCacheTtl
) {
}
