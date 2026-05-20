package com.kangworld.fantasybaseball.abuse.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ExpirationOptions;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.abuse.config.AbuseLimitPolicy;
import com.kangworld.fantasybaseball.abuse.exception.RateLimitExceededException;
import com.kangworld.fantasybaseball.abuse.exception.RateLimitUnavailableException;
import com.kangworld.fantasybaseball.abuse.service.AbuseRateLimiter;
import com.kangworld.fantasybaseball.common.time.FantasyTime;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FixedWindowAbuseRateLimiter implements AbuseRateLimiter {

    private static final String KEY_PREFIX = "abuse";
    private static final ExpirationOptions EXPIRE_ONLY_WHEN_ABSENT = ExpirationOptions.builder().nx().build();

    private final StringRedisTemplate stringRedisTemplate;
    private final FantasyTime fantasyTime;

    @Override
    public void check(String policyName, String key, AbuseLimitPolicy policy) {
        // FantasyTime의 Clock을 통해 테스트 가능한 현재 시각을 얻고, 별도 ZoneId 변환은 하지 않는다.
        Instant now = fantasyTime.getInstant();
        // 현재 시각이 속한 고정 window bucket을 Redis key에 포함한다.
        String redisKey = buildWindowKey(policyName, key, now, policy.getWindow());
        // INCR 결과가 이번 window의 요청 횟수다. TTL은 최초 생성 시 한 번만 window 길이로 설정한다.
        long count = increment(redisKey, policy.getWindow());

        // 허용 횟수를 넘긴 순간부터 같은 window 안의 요청은 429로 차단한다.
        if (count > policy.getMaxAttempts())
            throw new RateLimitExceededException();
    }

    private long increment(String redisKey, Duration ttl) {
        try {
            List<Object> results = stringRedisTemplate.execute(new SessionCallback<>() {

                @Override
                @SuppressWarnings({"unchecked", "NullableProblems"})
                public <K, V> List<Object> execute(RedisOperations<K, V> operations) {
                    RedisOperations<String, String> stringOperations = (RedisOperations<String, String>) operations;
                    try {
                        // INCR만 성공하고 EXPIRE가 빠지는 key 누수를 줄이기 위해 두 명령을 Redis transaction으로 묶는다.
                        stringOperations.multi();
                        stringOperations.opsForValue().increment(redisKey);
                        // NX 옵션으로 TTL이 없는 최초 요청에만 만료 시간을 설정해 매 요청마다 TTL이 뒤로 밀리지 않게 한다.
                        stringOperations.expire(redisKey, Expiration.from(ttl), EXPIRE_ONLY_WHEN_ABSENT);

                        return stringOperations.exec();
                    } catch (RuntimeException exception) {
                        stringOperations.discard();
                        throw exception;
                    }
                }
            });

            if (results == null || results.isEmpty() || results.get(0) == null)
                throw new RateLimitUnavailableException("요청 제한 확인에 실패했습니다.");

            return ((Number) results.get(0)).longValue();
        } catch (DataAccessException exception) {
            throw new RateLimitUnavailableException("요청 제한 확인에 실패했습니다.", exception);
        }
    }

    private String buildWindowKey(String policy, String key, Instant now, Duration window) {
        // 고정 윈도우 방식이므로 같은 window 안에서는 같은 key를 쓰고, 다음 window부터 새 key로 카운트한다.
        long bucket = now.getEpochSecond() / window.toSeconds();

        return "%s:%s:%s:%d".formatted(KEY_PREFIX, policy, key, bucket);
    }

}


