package com.kangworld.fantasybaseball.external.kbo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Ticker;
import com.kangworld.fantasybaseball.external.kbo.config.KboApiProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboLiveGameStateFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboLiveGameState;

class KboLiveGameStateFetchServiceTest {

    @Test
    void 같은_경기_상태는_TTL안에서_캐시를_재사용한다() {
        // given
        ManualTicker ticker = new ManualTicker();
        AtomicInteger callCount = new AtomicInteger();
        KboLiveGameStateFetcher fetcher = gameId -> {
            callCount.incrementAndGet();
            return KboLiveGameState.builder()
                    .gameId(gameId)
                    .status(KboGameStatus.PLAYING)
                    .build();
        };

        KboLiveGameStateFetchService service = new KboLiveGameStateFetchService(fetcher, buildProperties(), ticker);

        // when
        service.fetch("game-1");
        service.fetch("game-1");

        // then
        assertThat(callCount.get()).isEqualTo(1);
    }

    private KboApiProperties buildProperties() {
        return new KboApiProperties(
                null,
                null,
                null,
                null,
                java.time.Duration.ofSeconds(15),
                null
        );
    }

    private static class ManualTicker implements Ticker {
        @Override
        public long read() {
            return 0;
        }
    }

}
