package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;

class KboPlayerImageApiFetcherTest {

    @Test
    void fetch는_실제_JPEG_응답을_업로드용_asset으로_변환한다() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://image.example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboPlayerImageApiFetcher fetcher = new KboPlayerImageApiFetcher(builder.build());
        byte[] imageBytes = KboResponseFixtures.bytes("player-image-50500.jpg");

        assertThat(imageBytes).hasSize(5_349);
        assertThat(imageBytes).startsWith((byte) 0xff, (byte) 0xd8, (byte) 0xff);
        server.expect(requestTo("https://image.example.test/KBO_IMAGE/person/middle/2024/50500.jpg"))
                .andRespond(withSuccess(imageBytes, MediaType.IMAGE_JPEG));

        AssetFile image = fetcher.fetch(50_500L, 2024).orElseThrow();

        assertThat(image.bytes()).isEqualTo(imageBytes);
        assertThat(image.contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
        assertThat(image.extension()).isEqualTo("jpg");
        server.verify();
    }

}
