package com.kangworld.fantasybaseball.external.kbo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Ticker;
import com.kangworld.fantasybaseball.external.kbo.config.KboApiProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboScheduleFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;

class KboScheduleFetchServiceTest {

    @Test
    void 같은_날짜를_반복_조회하면_캐시를_재사용한다() {
        // given
        ManualTicker ticker = new ManualTicker();
        AtomicInteger callCount = new AtomicInteger();
        KboScheduleFetcher fetcher = (fromDate, toDate) -> {
            callCount.incrementAndGet();
            return List.of(buildGame("game-1", KboGameStatus.READY));
        };

        KboScheduleFetchService service = new KboScheduleFetchService(fetcher, buildProperties(), ticker);

        // when
        service.fetch(LocalDate.of(2026, 4, 28));
        service.fetch(LocalDate.of(2026, 4, 28));

        // then
        assertThat(callCount.get()).isEqualTo(1);
    }

    private KboApiProperties buildProperties() {
        return new KboApiProperties(
                null,
                null,
                Duration.ofMinutes(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(15),
                null
        );
    }

    private KboScheduleGame buildGame(String gameId, KboGameStatus status) {
        return KboScheduleGame.builder()
                .gameId(gameId)
                .startAt(LocalDateTime.of(2026, 4, 28, 14, 0))
                .status(status)
                .build();
    }

    private static class ManualTicker implements Ticker {
        @Override
        public long read() {
            return 0;
        }
    }

}
