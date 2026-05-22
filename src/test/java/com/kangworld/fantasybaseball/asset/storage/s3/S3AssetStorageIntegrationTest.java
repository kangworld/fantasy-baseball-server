package com.kangworld.fantasybaseball.asset.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;
import com.kangworld.fantasybaseball.asset.storage.AssetStorage;
import com.kangworld.fantasybaseball.asset.storage.config.AssetStorageProperties;
import com.kangworld.fantasybaseball.support.S3IntegrationTest;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@S3IntegrationTest
class S3AssetStorageIntegrationTest {

    @Autowired
    private AssetStorage assetStorage;

    @Autowired
    private AssetStorageProperties properties;

    @Autowired
    private S3Client s3Client;

    @Test
    void upload_manual_write_test() {
        // S3 콘솔에서 실제 object 생성을 확인하기 위한 임시 테스트다.
        String assetPath = "public/tmp/manual-write-test/manual-s3-write-test.jpg";
        byte[] bytes = "manual-s3-write-test".getBytes();
        AssetFile assetFile = AssetFile.builder()
                .bytes(bytes)
                .contentType("image/jpeg")
                .extension("jpg")
                .build();

        assetStorage.upload(assetPath, assetFile);

        assertThat(assetStorage.findBytes(assetPath)).contains(bytes);
    }

    @Test
    void upload_read_delete_test() {
        // given
        String assetPath = "public/tmp/integration-test/%s.jpg".formatted(java.util.UUID.randomUUID());
        byte[] bytes = "test-player-image".getBytes();
        AssetFile assetFile = AssetFile.builder()
                .bytes(bytes)
                .contentType("image/jpeg")
                .extension("jpg")
                .build();
        boolean deleted = false;

        try {
            // when
            assetStorage.upload(assetPath, assetFile);

            // then
            assertThat(assetStorage.findBytes(assetPath)).contains(bytes);

            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey(assetPath))
                    .build());
            assertThat(response.contentType()).isEqualTo("image/jpeg");
            assertThat(response.cacheControl()).isEqualTo(properties.getCacheControl());

            s3Client.deleteObject(deleteRequest(objectKey(assetPath)));
            deleted = true;
            assertThat(assetStorage.findBytes(assetPath)).isEmpty();
        } finally {
            if (!deleted)
                s3Client.deleteObject(deleteRequest(objectKey(assetPath)));
        }
    }

    private String objectKey(String assetPath) {
        String normalizedPrefix = properties.getPathPrefix().replaceAll("^/+|/+$", "");
        String normalizedPath = assetPath.replaceAll("^/+", "");
        if (normalizedPrefix.isBlank())
            return normalizedPath;

        return normalizedPrefix + "/" + normalizedPath;
    }

    private DeleteObjectRequest deleteRequest(String objectKey) {
        return DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();
    }

}
