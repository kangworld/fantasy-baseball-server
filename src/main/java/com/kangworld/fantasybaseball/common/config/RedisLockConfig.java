package com.kangworld.fantasybaseball.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.kangworld.rediscore.lock.RedisLockAspect;
import com.kangworld.rediscore.lock.RedisLockManager;
import com.kangworld.rediscore.support.RedisKeyFactory;

@Configuration
@EnableAspectJAutoProxy
public class RedisLockConfig {

    private static final String REDIS_NAMESPACE = "fantasy-baseball";

    @Bean
    public RedisKeyFactory redisKeyFactory() {

        return new RedisKeyFactory();
    }

    @Bean
    public DefaultRedisScript<Long> releaseLockScript() {
        // 락 해제는 "내가 잡은 락인지" 확인한 뒤 삭제해야 해서 Lua 스크립트로 원자 처리한다.
        DefaultRedisScript<Long> releaseLockScript = new DefaultRedisScript<>();
        releaseLockScript.setLocation(new ClassPathResource("scripts/release-lock.lua"));
        releaseLockScript.setResultType(Long.class);

        return releaseLockScript;
    }

    @Bean
    public RedisLockManager redisLockManager(
            StringRedisTemplate stringRedisTemplate,
            RedisKeyFactory redisKeyFactory,
            DefaultRedisScript<Long> releaseLockScript) {

        // 프로젝트별 네임스페이스를 고정해 다른 Redis 사용처와 락 키가 섞이지 않게 한다.
        return new RedisLockManager(stringRedisTemplate, redisKeyFactory, releaseLockScript, REDIS_NAMESPACE);
    }

    @Bean
    public RedisLockAspect redisLockAspect(RedisLockManager redisLockManager) {

        return new RedisLockAspect(redisLockManager);
    }

}
