package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboPlayerImageFetcher;

@Component
public class KboPlayerImageApiFetcher implements KboPlayerImageFetcher {

    private static final String PLAYER_IMAGE_PATH_FORMAT = "/KBO_IMAGE/person/middle/%d/%d.jpg";
    private static final String DEFAULT_CONTENT_TYPE = MediaType.IMAGE_JPEG_VALUE;
    private static final String DEFAULT_EXTENSION = "jpg";

    private final RestClient restClient;

    public KboPlayerImageApiFetcher(@Qualifier("kboImageRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<AssetFile> fetch(long playerId, int season) {
        try {
            // 현재 playerId가 KBO 공식 선수 id와 같아서 별도 매핑 없이 CDN 경로를 직접 구성한다.
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(PLAYER_IMAGE_PATH_FORMAT.formatted(season, playerId))
                    .retrieve()
                    .toEntity(byte[].class);

            byte[] body = response.getBody();
            if (body == null || body.length == 0)
                return Optional.empty();

            MediaType contentType = response.getHeaders().getContentType();
            return Optional.of(new AssetFile(
                    body,
                    contentType == null ? DEFAULT_CONTENT_TYPE : contentType.toString(),
                    DEFAULT_EXTENSION
            ));
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND)
                return Optional.empty();

            throw exception;
        }
    }

}
