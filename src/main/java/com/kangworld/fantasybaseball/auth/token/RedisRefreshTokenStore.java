package com.kangworld.fantasybaseball.auth.token;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenValue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(RefreshTokenValue refreshTokenValue) {
        String key = buildKey(refreshTokenValue.getUserId(), refreshTokenValue.getTokenId());
        String tokenSetKey = buildTokenSetKey(refreshTokenValue.getUserId());

        executeInTransaction(operations -> {
            // key에는 tokenId만 포함하고, value에는 secret hash만 저장한다.
            // 토큰 원문이 Redis에 남지 않도록 하기 위한 구조다.
            operations.opsForValue().set(
                    key,
                    refreshTokenValue.getSecretHash(),
                    refreshTokenValue.getTtl()
            );
            // 회원별 tokenId set은 전체 refresh token 삭제를 위한 보조 인덱스다.
            operations.opsForSet().add(tokenSetKey, refreshTokenValue.getTokenId());
            operations.expire(tokenSetKey, refreshTokenValue.getTtl());
        });
    }

    @Override
    public Optional<String> findSecretHash(Long userId, String tokenId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildKey(userId, tokenId)));
    }

    @Override
    public void delete(Long userId, String tokenId) {
        String key = buildKey(userId, tokenId);
        String tokenSetKey = buildTokenSetKey(userId);

        executeInTransaction(operations -> {
            operations.delete(key);
            operations.opsForSet().remove(tokenSetKey, tokenId);
        });
    }

    @Override
    public void deleteAll(Long userId) {
        String tokenSetKey = buildTokenSetKey(userId);
        Set<String> tokenIds = stringRedisTemplate.opsForSet().members(tokenSetKey);
        if (tokenIds == null || tokenIds.isEmpty())
            return;

        List<String> keys = new ArrayList<>();
        for (String tokenId : tokenIds)
            keys.add(buildKey(userId, tokenId));

        executeInTransaction(operations -> operations.delete(keys));
    }

    private void executeInTransaction(Consumer<RedisOperations<String, String>> command) {
        stringRedisTemplate.execute(new SessionCallback<List<Object>>() {

            @Override
            @SuppressWarnings({"unchecked", "NullableProblems"})
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) {
                RedisOperations<String, String> stringOperations = (RedisOperations<String, String>) operations;
                try {
                    stringOperations.multi();
                    command.accept(stringOperations);
                    return stringOperations.exec();
                } catch (RuntimeException exception) {
                    stringOperations.discard();
                    throw exception;
                }
            }
        });
    }

    private String buildKey(Long userId, String tokenId) {
        return "refresh:" + userId + ":" + tokenId;
    }

    private String buildTokenSetKey(Long userId) {
        return "refresh:" + userId + ":tokens";
    }

}
