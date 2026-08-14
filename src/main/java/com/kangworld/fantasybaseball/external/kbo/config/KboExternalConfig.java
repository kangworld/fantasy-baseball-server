package com.kangworld.fantasybaseball.external.kbo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.github.benmanes.caffeine.cache.Ticker;

@Configuration
@EnableConfigurationProperties(KboApiProperties.class)
public class KboExternalConfig {

    @Bean
    @Qualifier("kboRestClient")
    public RestClient kboRestClient(RestClient.Builder builder, KboApiProperties properties) {
        return builder
                .requestFactory(requestFactory())
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    @Qualifier("kboImageRestClient")
    public RestClient kboImageRestClient(RestClient.Builder builder, KboApiProperties properties) {
        // KBO 선수 이미지 CDN은 네이버 스포츠 API와 host가 달라서 별도 client로 분리한다.
        return builder
                .requestFactory(requestFactory())
                .baseUrl(properties.imageBaseUrl())
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        // 외부 응답이 멈춰도 정산과 로스터 요청이 무기한 대기하지 않도록 제한한다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return factory;
    }

    @Bean
    public Ticker kboTicker() {
        // 운영은 시스템 시간을 쓰고, 테스트는 필요하면 다른 ticker로 바꿔 끼울 수 있게 분리.
        return Ticker.systemTicker();
    }

}
