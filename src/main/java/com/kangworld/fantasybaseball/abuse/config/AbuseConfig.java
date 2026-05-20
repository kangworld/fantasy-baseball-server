package com.kangworld.fantasybaseball.abuse.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.kangworld.fantasybaseball.abuse.interceptor.AbuseRateLimitInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "abuse", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AbuseConfig implements WebMvcConfigurer {

    private final AbuseRateLimitInterceptor abuseRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(abuseRateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/actuator/health",
                        "/actuator/info"
                );
    }

}


