package com.kangworld.fantasybaseball.auth.token;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

        // key에는 tokenId만 포함하고, value에는 secret hash만 저장한다.
        // 토큰 원문이 Redis에 남지 않도록 하기 위한 구조다.
        stringRedisTemplate.opsForValue().set(
                key,
                refreshTokenValue.getSecretHash(),
                refreshTokenValue.getTtl()
        );
        // 회원별 tokenId set은 비밀번호 변경 시 해당 회원의 모든 refresh token을 지우기 위한 보조 인덱스다.
        stringRedisTemplate.opsForSet().add(tokenSetKey, refreshTokenValue.getTokenId());
        stringRedisTemplate.expire(tokenSetKey, refreshTokenValue.getTtl());
    }

    @Override
    public Optional<String> findSecretHash(Long userId, String tokenId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildKey(userId, tokenId)));
    }

    @Override
    public void delete(Long userId, String tokenId) {
        stringRedisTemplate.delete(buildKey(userId, tokenId));
        stringRedisTemplate.opsForSet().remove(buildTokenSetKey(userId), tokenId);
    }

    @Override
    public void deleteAll(Long userId) {
        String tokenSetKey = buildTokenSetKey(userId);
        Set<String> tokenIds = stringRedisTemplate.opsForSet().members(tokenSetKey);

        if (tokenIds == null || tokenIds.isEmpty()) {
            stringRedisTemplate.delete(tokenSetKey);
            return;
        }

        List<String> keys = new ArrayList<>();
        for (String tokenId : tokenIds) {
            keys.add(buildKey(userId, tokenId));
        }
        keys.add(tokenSetKey);

        stringRedisTemplate.delete(keys);
    }

    private String buildKey(Long userId, String tokenId) {
        return "refresh:" + userId + ":" + tokenId;
    }

    private String buildTokenSetKey(Long userId) {
        return "refresh:" + userId + ":tokens";
    }

}
