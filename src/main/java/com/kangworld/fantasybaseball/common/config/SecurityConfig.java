package com.kangworld.fantasybaseball.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.kangworld.fantasybaseball.auth.security.JwtAuthenticationFilter;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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

        // 인증이 비어 있거나 토큰이 유효하지 않으면 401을 반환.
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED)));

        // 회원가입, 로그인, 재발급, 헬스체크만 공개하고 나머지는 보호 API로 묶는다.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/health",
                        "/actuator/health",
                        "/actuator/info",
                        "/api/members/signup",
                        "/api/auth/login",
                        "/api/auth/refresh"
                ).permitAll()
                .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원가입 비밀번호는 BCrypt 해시로 저장.
        return new BCryptPasswordEncoder();
    }

}
