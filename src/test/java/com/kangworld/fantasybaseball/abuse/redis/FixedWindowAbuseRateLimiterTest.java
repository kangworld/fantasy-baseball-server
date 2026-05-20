package com.kangworld.fantasybaseball.abuse.redis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.ExpirationOptions;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.types.Expiration;

import com.kangworld.fantasybaseball.abuse.config.AbuseLimitPolicy;
import com.kangworld.fantasybaseball.abuse.exception.RateLimitExceededException;
import com.kangworld.fantasybaseball.abuse.exception.RateLimitUnavailableException;
import com.kangworld.fantasybaseball.common.time.FantasyTime;

@ExtendWith(MockitoExtension.class)
class FixedWindowAbuseRateLimiterTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    @InjectMocks
    private FixedWindowAbuseRateLimiter rateLimiter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisOperations<String, String> redisOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AbuseLimitPolicy policy;
    private FantasyTime fantasyTime;

    @BeforeEach
    void setUp() {
        policy = AbuseLimitPolicy.of(5, Duration.ofMinutes(1), false);
        fantasyTime = new FantasyTime(Clock.fixed(Instant.parse("2026-05-19T10:40:25Z"), SEOUL_ZONE_ID));
        rateLimiter = new FixedWindowAbuseRateLimiter(stringRedisTemplate, fantasyTime);
    }

    @Test
    void check는_window_bucket_key를_transaction으로_증가시킨다() {
        // given
        mockRedisTransaction(List.of(3L, true));
        long bucket = Instant.parse("2026-05-19T10:40:25Z").getEpochSecond() / 60;

        // when
        rateLimiter.check("login-email", "user@example.com", policy);

        // then
        verify(redisOperations).multi();
        verify(valueOperations).increment("abuse:login-email:user@example.com:" + bucket);
        verify(redisOperations).expire(
                "abuse:login-email:user@example.com:" + bucket,
                Expiration.from(Duration.ofMinutes(1)),
                ExpirationOptions.builder().nx().build()
        );
        verify(redisOperations).exec();
    }

    @Test
    void check는_count가_limit을_초과하면_예외를_던진다() {
        // given
        mockRedisTransaction(List.of(6L, true));

        // when then
        assertThatThrownBy(() -> rateLimiter.check("login-email", "user@example.com", policy))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void check는_exec_결과가_비어있으면_사용불가_예외를_던진다() {
        // given
        mockRedisTransaction(List.of());

        // when then
        assertThatThrownBy(() -> rateLimiter.check("login-email", "user@example.com", policy))
                .isInstanceOf(RateLimitUnavailableException.class);
    }

    private void mockRedisTransaction(List<Object> results) {
        doAnswer(invocation -> {
            SessionCallback<List<Object>> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        }).when(stringRedisTemplate).execute(any(SessionCallback.class));
        doReturn(valueOperations).when(redisOperations).opsForValue();
        doReturn(results).when(redisOperations).exec();
    }

}


