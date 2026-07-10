package com.kangworld.fantasybaseball.asset.storage.s3;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;
import com.kangworld.fantasybaseball.asset.storage.AssetStorage;
import com.kangworld.fantasybaseball.asset.storage.config.AssetStorageProperties;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3AssetStorage implements AssetStorage {

    private final S3Client s3Client;
    private final AssetStorageProperties properties;

    @Override
    public Optional<byte[]> findBytes(String assetPath) {
        try {
            String objectKey = buildObjectKey(assetPath);
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());

            return Optional.of(response.asByteArray());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void upload(String assetPath, AssetFile assetFile) {
        String objectKey = buildObjectKey(assetPath);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(assetFile.contentType())
                // S3는 object metadata에 Cache-Control을 저장하고, 이후 asset 응답 헤더로 그대로 내려준다.
                .cacheControl(properties.cacheControl())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(assetFile.bytes()));
    }

    private String buildObjectKey(String assetPath) {
        String normalizedPrefix = properties.pathPrefix().replaceAll("^/+|/+$", "");
        String normalizedPath = assetPath.replaceAll("^/+", "");
        if (normalizedPrefix.isBlank())
            return normalizedPath;

        return normalizedPrefix + "/" + normalizedPath;
    }

}
