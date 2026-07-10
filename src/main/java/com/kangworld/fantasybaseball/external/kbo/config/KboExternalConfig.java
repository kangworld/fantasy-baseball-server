package com.kangworld.fantasybaseball.external.kbo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.github.benmanes.caffeine.cache.Ticker;

@Configuration
@EnableConfigurationProperties(KboApiProperties.class)
public class KboExternalConfig {

    @Bean
    @Qualifier("kboRestClient")
    public RestClient kboRestClient(RestClient.Builder builder, KboApiProperties properties) {
        // KBO 호출은 baseUrl만 공통으로 고정하고, 경로/쿼리는 fetcher가 책임지게 둔다.
        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    @Qualifier("kboImageRestClient")
    public RestClient kboImageRestClient(RestClient.Builder builder, KboApiProperties properties) {
        // KBO 선수 이미지 CDN은 네이버 스포츠 API와 host가 달라서 별도 client로 분리한다.
        return builder
                .baseUrl(properties.imageBaseUrl())
                .build();
    }

    @Bean
    public Ticker kboTicker() {
        // 운영은 시스템 시간을 쓰고, 테스트는 필요하면 다른 ticker로 바꿔 끼울 수 있게 분리.
        return Ticker.systemTicker();
    }

}
