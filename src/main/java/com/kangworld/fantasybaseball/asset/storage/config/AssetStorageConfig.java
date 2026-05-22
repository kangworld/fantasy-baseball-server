package com.kangworld.fantasybaseball.asset.storage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(AssetStorageProperties.class)
public class AssetStorageConfig {

    @Bean
    public S3Client assetS3Client(AssetStorageProperties properties) {
        // AWS SDK 기본 credential chain을 사용한다. 로컬은 AWS_PROFILE, 운영은 IAM Role을 통해 인증한다.
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }

}
