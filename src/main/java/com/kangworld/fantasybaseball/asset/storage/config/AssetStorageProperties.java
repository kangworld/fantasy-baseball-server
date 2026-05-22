package com.kangworld.fantasybaseball.asset.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "asset.storage.s3")
public class AssetStorageProperties {

    private String bucket;
    private String region;
    private String pathPrefix;
    private String cacheControl;

    public String getPublicBaseUrl() {
        String normalizedPrefix = pathPrefix.replaceAll("^/+|/+$", "");
        String baseUrl = "https://%s.s3.%s.amazonaws.com".formatted(bucket, region);
        if (normalizedPrefix.isBlank())
            return baseUrl;

        return baseUrl + "/" + normalizedPrefix;
    }

}
