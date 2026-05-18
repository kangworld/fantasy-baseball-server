package com.kangworld.fantasybaseball.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Test
    void save는_refreshToken을_저장하고_회원별_tokenId_목록을_갱신한다() {
        // given
        Duration ttl = Duration.ofDays(14);
        RefreshTokenValue value = RefreshTokenValue.builder()
                .userId(1L)
                .tokenId("token-id")
                .secretHash("secret-hash")
                .ttl(ttl)
                .build();
        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();

        // when
        refreshTokenStore.save(value);

        // then
        verify(valueOperations).set("refresh:1:token-id", "secret-hash", ttl);
        verify(setOperations).add("refresh:1:tokens", "token-id");
        verify(stringRedisTemplate).expire("refresh:1:tokens", ttl);
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
    void delete는_refreshToken과_회원별_tokenId_목록을_함께_정리한다() {
        // given
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();

        // when
        refreshTokenStore.delete(1L, "token-id");

        // then
        verify(stringRedisTemplate).delete("refresh:1:token-id");
        verify(setOperations).remove("refresh:1:tokens", "token-id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteAll은_회원의_모든_refreshToken을_삭제한다() {
        // given
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();
        doReturn(Set.of("token-a", "token-b")).when(setOperations).members("refresh:1:tokens");

        // when
        refreshTokenStore.deleteAll(1L);

        // then
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(stringRedisTemplate).delete(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                "refresh:1:token-a",
                "refresh:1:token-b",
                "refresh:1:tokens"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteAll은_tokenId_목록이_없어도_인덱스_key를_정리한다() {
        // given
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();
        doReturn(Set.of()).when(setOperations).members("refresh:1:tokens");

        // when
        refreshTokenStore.deleteAll(1L);

        // then
        verify(stringRedisTemplate).delete("refresh:1:tokens");
        verify(stringRedisTemplate, org.mockito.Mockito.never()).delete(any(Collection.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteAll은_redis가_null을_반환해도_인덱스_key를_정리한다() {
        // given
        doReturn(setOperations).when(stringRedisTemplate).opsForSet();
        doReturn(null).when(setOperations).members("refresh:1:tokens");

        // when
        refreshTokenStore.deleteAll(1L);

        // then
        verify(stringRedisTemplate).delete("refresh:1:tokens");
        verify(stringRedisTemplate, org.mockito.Mockito.never()).delete(any(Collection.class));
    }

}
