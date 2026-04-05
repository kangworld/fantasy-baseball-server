package com.kangworld.fantasybaseball.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // csrf는 브라우저 폼 기반 애플리케이션이 아니라 API 서버 기준이라 비활성화.
        http.csrf(AbstractHttpConfigurer::disable);

        // formLogin은 기본 로그인 페이지가 필요 없어서 비활성화.
        http.formLogin(AbstractHttpConfigurer::disable);

        // httpBasic은 브라우저 기본 인증 팝업을 사용하지 않을 거라 비활성화.
        http.httpBasic(AbstractHttpConfigurer::disable);

        // health 경로는 서버 상태 확인이 필요해서 permitAll.
        // 나머지 API는 인증 기능이 들어가기 전까지 permitAll.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/actuator/health", "/actuator/info").permitAll()
                .anyRequest().permitAll());

        return http.build();
    }

}
