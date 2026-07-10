package com.kangworld.fantasybaseball.abuse.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import com.kangworld.fantasybaseball.abuse.AbuseLimit;
import com.kangworld.fantasybaseball.abuse.config.AbuseLimitPolicy;
import com.kangworld.fantasybaseball.abuse.exception.RateLimitUnavailableException;
import com.kangworld.fantasybaseball.abuse.service.AbuseRateLimiter;
import com.kangworld.fantasybaseball.auth.security.AuthenticatedMember;

@ExtendWith(MockitoExtension.class)
class AbuseRateLimitInterceptorTest {

    @InjectMocks
    private AbuseRateLimitInterceptor interceptor;

    @Mock
    private AbuseRateLimiter abuseRateLimiter;

    @BeforeEach
    void setUp() {
        interceptor = new AbuseRateLimitInterceptor(abuseRateLimiter);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle은_인증_사용자면_memberId를_key로_조회_API를_제한한다() {
        // given
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedMember(7L, "USER"),
                null
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ranks/daily");

        // when
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // then
        assertThat(result).isTrue();
        verify(abuseRateLimiter).check(any(), eq("member:7"), any(AbuseLimitPolicy.class));
    }

    @Test
    void preHandle은_비인증_사용자면_remoteAddr를_key로_사용한다() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games/team-ranks");
        request.setRemoteAddr("127.0.0.1");

        // when
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // then
        assertThat(result).isTrue();
        verify(abuseRateLimiter).check(any(), eq("ip:127.0.0.1"), any(AbuseLimitPolicy.class));
    }

    @Test
    void preHandle은_failOpen_정책이면_Redis_실패에도_API를_처리한다() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/scoreboard/me");
        request.setRemoteAddr("127.0.0.1");
        doThrow(new RateLimitUnavailableException("redis down"))
                .when(abuseRateLimiter).check(any(), eq("ip:127.0.0.1"), any(AbuseLimitPolicy.class));

        // when then
        assertThatCode(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void preHandle은_AbuseLimit이_붙은_API는_중복_제한하지_않는다() throws NoSuchMethodException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        // when
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), abuseLimitedHandler());

        // then
        assertThat(result).isTrue();
        verifyNoInteractions(abuseRateLimiter);
    }

    @Test
    void preHandle은_프록시_헤더의_첫번째_IP를_key로_사용한다() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games/team-ranks");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");

        // when
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // then
        assertThat(result).isTrue();
        verify(abuseRateLimiter).check(any(), eq("ip:10.0.0.1"), any(AbuseLimitPolicy.class));
    }

    private HandlerMethod abuseLimitedHandler() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("login");
        return new HandlerMethod(new TestController(), method);
    }

    private static class TestController {

        @AbuseLimit(key = "'login'")
        public void login() {
        }

    }

}


