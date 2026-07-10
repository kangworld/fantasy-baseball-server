package com.kangworld.fantasybaseball.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kangworld.fantasybaseball.auth.cookie.AuthCookieFactory;
import com.kangworld.fantasybaseball.auth.token.JwtAccessTokenParser;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAccessTokenParser jwtAccessTokenParser;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Cookie accessTokenCookie = WebUtils.getCookie(request, AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME);

        if (accessTokenCookie == null || accessTokenCookie.getValue().isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = accessTokenCookie.getValue();

        jwtAccessTokenParser.parse(accessToken).ifPresent(authenticatedMember -> {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    authenticatedMember,
                    null,
                    List.of(new SimpleGrantedAuthority(authenticatedMember.role()))
            );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        });

        filterChain.doFilter(request, response);
    }

}
