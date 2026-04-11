package com.kangworld.fantasybaseball.auth.token;

import java.util.Optional;

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

        stringRedisTemplate.opsForValue().set(
                key,
                refreshTokenValue.getSecretHash(),
                refreshTokenValue.getTtl()
        );
    }

    @Override
    public Optional<String> findSecretHash(Long userId, String tokenId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildKey(userId, tokenId)));
    }

    @Override
    public void delete(Long userId, String tokenId) {
        stringRedisTemplate.delete(buildKey(userId, tokenId));
    }

    private String buildKey(Long userId, String tokenId) {
        return "refresh:" + userId + ":" + tokenId;
    }

}
