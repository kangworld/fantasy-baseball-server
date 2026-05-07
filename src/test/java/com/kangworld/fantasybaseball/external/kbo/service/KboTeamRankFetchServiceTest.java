package com.kangworld.fantasybaseball.external.kbo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.kangworld.fantasybaseball.external.kbo.config.KboApiProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboTeamRankFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboTeamRank;

class KboTeamRankFetchServiceTest {

    @Test
    void 같은_시즌을_반복_조회하면_캐시를_재사용한다() {
        // given
        AtomicInteger callCount = new AtomicInteger();
        KboTeamRankFetcher fetcher = season -> {
            callCount.incrementAndGet();
            return List.of(buildRank());
        };
        KboTeamRankFetchService service = new KboTeamRankFetchService(fetcher, buildProperties(), () -> 0);

        // when
        service.fetch(2026);
        service.fetch(2026);

        // then
        assertThat(callCount.get()).isEqualTo(1);
    }

    private KboApiProperties buildProperties() {
        KboApiProperties properties = new KboApiProperties();
        properties.setTeamRankCacheTtl(Duration.ofSeconds(30));
        return properties;
    }

    private KboTeamRank buildRank() {
        return KboTeamRank.builder()
                .ranking(1)
                .teamCode("KT")
                .build();
    }

}
