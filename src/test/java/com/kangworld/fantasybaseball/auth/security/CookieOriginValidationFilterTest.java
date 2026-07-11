package com.kangworld.fantasybaseball.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangworld.fantasybaseball.auth.config.AuthSecurityProperties;
import com.kangworld.fantasybaseball.auth.cookie.AuthCookieFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;

class CookieOriginValidationFilterTest {

    private final CookieOriginValidationFilter filter = new CookieOriginValidationFilter(
            new ObjectMapper(),
            new AuthSecurityProperties(
                    new AuthSecurityProperties.Jwt("test-secret", 3600, 1209600),
                    new AuthSecurityProperties.AuthCookie(true),
                    new AuthSecurityProperties.Cors(List.of("http://localhost:5173")))
    );

    @Test
    void 허용된_origin의_상태변경_쿠키요청은_통과한다() throws Exception {
        MockHttpServletRequest request = authCookieRequest("POST");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 허용된_referer의_상태변경_쿠키요청은_통과한다() throws Exception {
        MockHttpServletRequest request = authCookieRequest("PUT");
        request.addHeader(HttpHeaders.REFERER, "http://localhost:5173/roster/edit");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 출처가_없는_상태변경_쿠키요청은_403으로_차단한다() throws Exception {
        MockHttpServletRequest request = authCookieRequest("POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("허용되지 않은 요청 출처입니다.");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 허용되지_않은_origin의_상태변경_쿠키요청은_403으로_차단한다() throws Exception {
        MockHttpServletRequest request = authCookieRequest("DELETE");
        request.addHeader(HttpHeaders.ORIGIN, "https://evil.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 안전한_GET_쿠키요청은_origin_없이_통과한다() throws Exception {
        MockHttpServletRequest request = authCookieRequest("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest authCookieRequest(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/rosters/me");
        request.setCookies(new Cookie(AuthCookieFactory.ACCESS_TOKEN_COOKIE_NAME, "access-token"));
        return request;
    }

}
