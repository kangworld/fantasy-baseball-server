package com.kangworld.fantasybaseball.asset.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset.storage.s3")
public record AssetStorageProperties(
        String bucket,
        String region,
        String pathPrefix,
        String cacheControl
) {

    public String publicBaseUrl() {
        String normalizedPrefix = pathPrefix.replaceAll("^/+|/+$", "");
        String baseUrl = "https://%s.s3.%s.amazonaws.com".formatted(bucket, region);
        if (normalizedPrefix.isBlank())
            return baseUrl;

        return baseUrl + "/" + normalizedPrefix;
    }

}
