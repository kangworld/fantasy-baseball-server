package com.kangworld.fantasybaseball.external.kbo.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.kbo")
public class KboApiProperties {

    private String baseUrl;
    private Duration scheduleCacheTtl;
    private Duration recordCacheTtl;
    private Duration liveGameStateCacheTtl;

}
