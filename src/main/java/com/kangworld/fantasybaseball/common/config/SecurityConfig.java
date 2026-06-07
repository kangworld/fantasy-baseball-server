package com.kangworld.fantasybaseball.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.kangworld.fantasybaseball.auth.security.JwtAuthenticationFilter;
import com.kangworld.fantasybaseball.common.api.ApiAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            @Value("#{'${security.cors.allowed-origins}'.split(',')}") List<String> allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiAuthenticationEntryPoint = apiAuthenticationEntryPoint;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 로컬 Vue 개발 서버에서 API를 직접 호출할 수 있도록 CORS preflight를 허용한다.
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // csrf는 브라우저 폼 기반 애플리케이션이 아니라 API 서버 기준이라 비활성화.
        http.csrf(AbstractHttpConfigurer::disable);

        // formLogin은 기본 로그인 페이지가 필요 없어서 비활성화.
        http.formLogin(AbstractHttpConfigurer::disable);

        // httpBasic은 브라우저 기본 인증 팝업을 사용하지 않을 거라 비활성화.
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 세션을 쓰지 않고 매 요청마다 JWT로 인증을 확인하도록 stateless로 설정.
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 인증이 필요한 API에 토큰 검증을 먼저 태우기 위해 JWT 필터를 추가.
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 인증이 비어 있거나 토큰이 유효하지 않으면 API 공통 포맷으로 401을 반환.
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(apiAuthenticationEntryPoint));

        // 인증/헬스체크와 공개 조회 API만 열고, 개인화 API는 보호 API로 묶는다.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/health",
                        "/api/app-config",
                        "/actuator/health",
                        "/actuator/info",
                        "/api/members/signup",
                        "/api/members/signup/email-verification",
                        "/api/members/signup/email-verification/confirm",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/rosters/config",
                        "/api/ranks/daily",
                        "/api/ranks/season"
                ).permitAll()
                .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원가입 비밀번호는 BCrypt 해시로 저장.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

}
