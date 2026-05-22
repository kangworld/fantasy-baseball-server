package com.kangworld.fantasybaseball.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper apiObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // redis-core의 기본 ObjectMapper가 Java time 모듈 없는 상태로 API 직렬화에 쓰이지 않도록 우선순위를 고정한다.
        return builder.createXmlMapper(false)
                .build();
    }

}
