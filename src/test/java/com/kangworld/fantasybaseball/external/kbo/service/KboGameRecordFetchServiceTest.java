package com.kangworld.fantasybaseball.external.kbo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Ticker;
import com.kangworld.fantasybaseball.external.kbo.config.KboApiProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboGameRecordFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;

class KboGameRecordFetchServiceTest {

    @Test
    void 같은_경기_기록은_TTL안에서_캐시를_재사용한다() {
        // given
        ManualTicker ticker = new ManualTicker();
        AtomicInteger callCount = new AtomicInteger();
        KboGameRecordFetcher fetcher = gameId -> {
            callCount.incrementAndGet();
            return KboGameRecord.builder()
                    .gameId(gameId)
                    .status(KboGameStatus.FINISHED)
                    .batterRecords(List.of())
                    .pitcherRecords(List.of())
                    .build();
        };

        KboGameRecordFetchService service = new KboGameRecordFetchService(fetcher, buildProperties(), ticker);

        // when
        service.fetch("game-1");
        service.fetch("game-1");

        // then
        assertThat(callCount.get()).isEqualTo(1);
    }

    private KboApiProperties buildProperties() {
        KboApiProperties properties = new KboApiProperties();
        properties.setRecordCacheTtl(java.time.Duration.ofSeconds(30));
        return properties;
    }

    private static class ManualTicker implements Ticker {
        @Override
        public long read() {
            return 0;
        }
    }

}
