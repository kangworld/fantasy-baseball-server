package com.kangworld.fantasybaseball.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class RedisContainerTestConfig {

    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                .withExposedPorts(6379);
    }

    @Bean
    public DynamicPropertyRegistrar redisPropertiesRegistrar(GenericContainer<?> redisContainer) {
        return registry -> {
            // redis-core 설정 중 Docker가 동적으로 결정한 접속 주소만 테스트 컨테이너 값으로 바꾼다.
            registry.add("redis.core.host", redisContainer::getHost);
            registry.add("redis.core.port", redisContainer::getFirstMappedPort);
        };
    }

}
