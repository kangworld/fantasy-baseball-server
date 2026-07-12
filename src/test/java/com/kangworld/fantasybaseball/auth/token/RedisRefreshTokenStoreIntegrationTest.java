package com.kangworld.fantasybaseball.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenValue;
import com.kangworld.fantasybaseball.support.RedisContainerIntegrationTest;

@RedisContainerIntegrationTest
class RedisRefreshTokenStoreIntegrationTest {

    @Autowired
    private RedisRefreshTokenStore refreshTokenStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        RedisConnection connection = connectionFactory.getConnection();
        try {
            connection.serverCommands().flushDb();
        } finally {
            connection.close();
        }
    }

    @Test
    void save는_실제_Redis에_토큰과_회원별_인덱스_TTL을_저장한다() {
        // given
        Duration ttl = Duration.ofMinutes(10);
        RefreshTokenValue token = new RefreshTokenValue(
                1L,
                "token-id",
                "raw-token",
                "secret-hash",
                ttl);

        // when
        refreshTokenStore.save(token);

        // then
        assertThat(redisTemplate.opsForValue().get("refresh:1:token-id"))
                .isEqualTo("secret-hash");
        assertThat(redisTemplate.opsForSet().isMember("refresh:1:tokens", "token-id"))
                .isTrue();
        assertThat(redisTemplate.getExpire("refresh:1:token-id", TimeUnit.SECONDS))
                .isBetween(1L, ttl.toSeconds());
        assertThat(redisTemplate.getExpire("refresh:1:tokens", TimeUnit.SECONDS))
                .isBetween(1L, ttl.toSeconds());
    }

    @Test
    void delete는_실제_Redis에서_토큰과_회원별_인덱스를_함께_제거한다() {
        // given
        RefreshTokenValue token = new RefreshTokenValue(
                1L,
                "token-id",
                "raw-token",
                "secret-hash",
                Duration.ofMinutes(10));
        refreshTokenStore.save(token);

        // when
        refreshTokenStore.delete(1L, "token-id");

        // then
        assertThat(redisTemplate.hasKey("refresh:1:token-id")).isFalse();
        assertThat(redisTemplate.opsForSet().isMember("refresh:1:tokens", "token-id"))
                .isFalse();
    }

}
