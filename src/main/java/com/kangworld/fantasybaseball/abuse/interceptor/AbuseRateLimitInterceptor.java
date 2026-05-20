package com.kangworld.fantasybaseball.abuse.interceptor;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.kangworld.fantasybaseball.abuse.AbuseLimit;
import com.kangworld.fantasybaseball.abuse.config.AbuseLimitPolicy;
import com.kangworld.fantasybaseball.abuse.exception.RateLimitUnavailableException;
import com.kangworld.fantasybaseball.abuse.service.AbuseRateLimiter;
import com.kangworld.fantasybaseball.auth.security.AuthenticatedMember;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "abuse", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AbuseRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AbuseRateLimitInterceptor.class);
    private static final String DEFAULT_API_POLICY = "default-api";
    private static final AbuseLimitPolicy DEFAULT_API_LIMIT_POLICY = AbuseLimitPolicy.of(300, Duration.ofMinutes(1), true);
    // 앞단 프록시가 원본 클라이언트 IP를 전달할 때 쓰는 후보 헤더다. Proxy-Client-IP 계열도 프록시 IP가 아니라 프록시가 전달한 클라이언트 IP를 의미한다.
    private static final List<String> IP_HEADERS = List.of(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "X-Real-IP"
    );

    private final AbuseRateLimiter abuseRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isHandledByAbuseLimit(handler))
            return true;

        // @AbuseLimit로 별도 정책을 선언하지 않은 일반 API는 기본 정책으로 일괄 제한한다.
        String key = resolveKey(request);

        try {
            abuseRateLimiter.check(DEFAULT_API_POLICY, key, DEFAULT_API_LIMIT_POLICY);
        } catch (RateLimitUnavailableException exception) {
            if (!DEFAULT_API_LIMIT_POLICY.isFailOpen())
                throw exception;

            log.warn("rate limit check failed. policy={}, key={}, error={}", DEFAULT_API_POLICY, key, exception.getMessage());
        }

        return true;
    }

    private boolean isHandledByAbuseLimit(Object handler) {
        // 컨트롤러 메서드에 @AbuseLimit가 붙은 API는 AOP에서 세부 정책으로 처리하므로 기본 정책을 중복 적용하지 않는다.
        return handler instanceof HandlerMethod handlerMethod
                && handlerMethod.hasMethodAnnotation(AbuseLimit.class);
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)
            return "member:" + authenticatedMember.getMemberId();

        return "ip:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        return IP_HEADERS.stream()
                .map(request::getHeader)
                .filter(StringUtils::hasText)
                .map(this::getFirstIp)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(request.getRemoteAddr());
    }

    private String getFirstIp(String headerValue) {
        // X-Forwarded-For는 "client, proxy1, proxy2" 형태가 될 수 있어서 최초 클라이언트 IP만 사용한다. 단일 IP 헤더에도 같은 처리를 적용해도 값은 그대로다.
        return headerValue.split(",")[0].trim();
    }

}


