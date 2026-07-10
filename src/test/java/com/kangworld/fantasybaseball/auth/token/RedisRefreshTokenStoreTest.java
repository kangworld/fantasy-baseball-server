package com.kangworld.fantasybaseball.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenValue;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

    @InjectMocks
    private RedisRefreshTokenStore refreshTokenStore;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisOperations<String, String> redisOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Test
    void save는_transaction으로_refreshToken과_회원별_tokenId_목록을_함께_갱신한다() {
        // given
        mockRedisTransaction();
        Duration ttl = Duration.ofDays(14);
        RefreshTokenValue value = new RefreshTokenValue(1L, "token-id", null, "secret-hash", ttl);

        // when
        refreshTokenStore.save(value);

        // then
        verify(redisOperations).multi();
        verify(valueOperations).set("refresh:1:token-id", "secret-hash", ttl);
        verify(setOperations).add("refresh:1:tokens", "token-id");
        verify(redisOperations).expire("refresh:1:tokens", ttl);
        verify(redisOperations).exec();
    }

    @Test
    void save는_transaction_구성중_실패하면_discard하고_exec하지_않는다() {
        // given
        mockRedisTransaction();
        Duration ttl = Duration.ofDays(14);
        RefreshTokenValue value = new RefreshTokenValue(1L, "token-id", null, "secret-hash", ttl);
        doThrow(new IllegalStateException("redis command failed"))
                .when(redisOperations).expire("refresh:1:tokens", ttl);

        // when then
        assertThatThrownBy(() -> refreshTokenStore.save(value))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis command failed");
        verify(redisOperations).discard();
        verify(redisOperations, never()).exec();
    }

    @Test
    void findSecretHash는_tokenId에_해당하는_secretHash를_조회한다() {
        // given
        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        doReturn("secret-hash").when(valueOperations).get("refresh:1:token-id");

        // when
        Optional<String> result = refreshTokenStore.findSecretHash(1L, "token-id");

        // then
        assertThat(result).contains("secret-hash");
    }

    @Test
    void delete는_transaction으로_refreshToken과_회원별_tokenId_목록을_함께_정리한다() {
        // given
        mockRedisTransaction();

        // when
        refreshTokenStore.delete(1L, "token-id");

        // then
        verify(redisOperations).multi();
        verify(redisOperations).delete("refresh:1:token-id");
        verify(setOperations).remove("refresh:1:tokens", "token-id");
        verify(redisOperations).exec();
    }

    @Test
    void delete는_transaction_구성중_실패하면_discard하고_exec하지_않는다() {
        // given
        mockRedisTransaction();
        doThrow(new IllegalStateException("redis command failed"))
                .when(setOperations).remove("refresh:1:tokens", "token-id");

        // when then
        assertThatThrownBy(() -> refreshTokenStore.delete(1L, "token-id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis command failed");
        verify(redisOperations).discard();
        verify(redisOperations, never()).exec();
    }

    @Test
    void deleteAll은_회원의_실제_refreshToken_key만_transaction으로_삭제한다() {
        // given
        mockRedisTransaction();
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();
        doReturn(Set.of("token-a", "token-b")).when(setOperations).members("refresh:1:tokens");

        // when
        refreshTokenStore.deleteAll(1L);

        // then
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(redisOperations).multi();
        verify(redisOperations).delete(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                "refresh:1:token-a",
                "refresh:1:token-b"
        );
        verify(redisOperations, never()).delete("refresh:1:tokens");
        verify(redisOperations).exec();
    }

    @Test
    void deleteAll은_tokenId_목록이_없으면_transaction을_열지_않는다() {
        // given
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();
        doReturn(Set.of()).when(setOperations).members("refresh:1:tokens");

        // when
        refreshTokenStore.deleteAll(1L);

        // then
        verify(stringRedisTemplate, never()).execute(any(SessionCallback.class));
    }

    @Test
    void deleteAll은_tokenId_목록_조회가_null이면_transaction을_열지_않는다() {
        // given
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();
        doReturn(null).when(setOperations).members("refresh:1:tokens");

        // when
        refreshTokenStore.deleteAll(1L);

        // then
        verify(stringRedisTemplate, never()).execute(any(SessionCallback.class));
    }

    @Test
    void deleteAll은_transaction_구성중_실패하면_discard하고_exec하지_않는다() {
        // given
        mockRedisTransaction();
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();
        doReturn(Set.of("token-a")).when(setOperations).members("refresh:1:tokens");
        doThrow(new IllegalStateException("redis command failed"))
                .when(redisOperations).delete(any(Collection.class));

        // when then
        assertThatThrownBy(() -> refreshTokenStore.deleteAll(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis command failed");
        verify(redisOperations).discard();
        verify(redisOperations, never()).exec();
    }

    private void mockRedisTransaction() {
        doAnswer(invocation -> {
            SessionCallback<List<Object>> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        }).when(stringRedisTemplate).execute(any(SessionCallback.class));
        lenient().doReturn(valueOperations).when(redisOperations).opsForValue();
        lenient().doReturn(setOperations).when(redisOperations).opsForSet();
        lenient().doReturn(List.of("OK")).when(redisOperations).exec();
    }

}
