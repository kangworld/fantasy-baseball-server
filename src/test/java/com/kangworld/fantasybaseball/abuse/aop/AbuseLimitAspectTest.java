package com.kangworld.fantasybaseball.abuse.aop;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.kangworld.fantasybaseball.abuse.AbuseLimit;
import com.kangworld.fantasybaseball.abuse.service.AbuseRateLimiter;
import com.kangworld.fantasybaseball.auth.service.dto.LoginRequest;

@SpringBootTest(
        classes = {
                AbuseLimitAspect.class,
                AbuseLimitAspectTest.TestService.class,
                AbuseLimitAspectTest.TestAopConfig.class
        },
        properties = {
                "abuse.enabled=true"
        }
)
@ActiveProfiles("test")
class AbuseLimitAspectTest {

    @Autowired
    private TestService testService;

    @MockitoBean
    private AbuseRateLimiter abuseRateLimiter;

    @Test
    void AbuseLimit은_SpEL로_key를_평가해_rateLimiter를_호출한다() {
        // given
        LoginRequest request = new LoginRequest("aspect@example.com", "password");

        // when
        testService.login(request);

        // then
        verify(abuseRateLimiter).check(
                eq("login-email"),
                eq("aspect@example.com"),
                argThat(policy -> policy.getMaxAttempts() == 60 && policy.getWindow().toMinutes() == 1 && !policy.isFailOpen())
        );
    }

    @Service
    static class TestService {

        @AbuseLimit(policy = "login-email", key = "#request.email")
        public void login(LoginRequest request) {
        }

    }

    @EnableAspectJAutoProxy
    static class TestAopConfig {
    }

}


