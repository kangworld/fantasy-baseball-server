package com.kangworld.fantasybaseball.auth.security;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangworld.fantasybaseball.auth.config.AuthSecurityProperties;
import com.kangworld.fantasybaseball.auth.cookie.AuthCookieFactory;
import com.kangworld.fantasybaseball.common.api.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieOriginValidationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;

    public CookieOriginValidationFilter(
            ObjectMapper objectMapper,
            AuthSecurityProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.allowedOrigins = properties.cors().allowedOrigins();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isSafeMethod(request.getMethod()) || !hasAuthCookie(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestOrigin = resolveRequestOrigin(request);

        if (requestOrigin == null || !allowedOrigins.contains(requestOrigin)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getOutputStream(),
                    ApiResponse.of(HttpStatus.FORBIDDEN, "허용되지 않은 요청 출처입니다.", null)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSafeMethod(String method) {
        return HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method)
                || HttpMethod.TRACE.matches(method);
    }

    private boolean hasAuthCookie(HttpServletRequest request) {
        return WebUtils.getCookie(request, AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME) != null
                || WebUtils.getCookie(request, AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME) != null;
    }

    private String resolveRequestOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank()) {
            return origin;
        }

        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return null;
        }

        try {
            URI refererUri = URI.create(referer);
            if (refererUri.getScheme() == null || refererUri.getHost() == null) {
                return null;
            }

            String authority = refererUri.getPort() < 0
                    ? refererUri.getHost()
                    : refererUri.getHost() + ":" + refererUri.getPort();
            return refererUri.getScheme() + "://" + authority;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

}
