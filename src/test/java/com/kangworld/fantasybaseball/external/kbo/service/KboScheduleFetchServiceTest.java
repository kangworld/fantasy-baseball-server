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

    @Test
    void liveGame필터는_PLAYING만_남긴다() {
        // given
        ManualTicker ticker = new ManualTicker();
        KboScheduleFetcher fetcher = (fromDate, toDate) -> List.of(
                buildGame("game-1", KboGameStatus.READY),
                buildGame("game-2", KboGameStatus.PLAYING),
                buildGame("game-3", KboGameStatus.FINISHED)
        );

        KboScheduleFetchService service = new KboScheduleFetchService(fetcher, buildProperties(), ticker);

        // when
        List<KboScheduleGame> liveGames = service.findLiveGames(LocalDate.of(2026, 4, 28));

        // then
        assertThat(liveGames).extracting(KboScheduleGame::getGameId)
                .containsExactly("game-2");
    }

    @Test
    void recordTarget필터는_진행중_종료_서스펜디드만_남긴다() {
        // given
        ManualTicker ticker = new ManualTicker();
        KboScheduleFetcher fetcher = (fromDate, toDate) -> List.of(
                buildGame("game-1", KboGameStatus.READY),
                buildGame("game-2", KboGameStatus.PLAYING),
                buildGame("game-3", KboGameStatus.FINISHED),
                buildGame("game-4", KboGameStatus.SUSPENDED)
        );

        KboScheduleFetchService service = new KboScheduleFetchService(fetcher, buildProperties(), ticker);

        // when
        List<KboScheduleGame> targetGames = service.findRecordTargetGames(LocalDate.of(2026, 4, 28));

        // then
        assertThat(targetGames).extracting(KboScheduleGame::getGameId)
                .containsExactly("game-2", "game-3", "game-4");
    }

    private KboApiProperties buildProperties() {
        KboApiProperties properties = new KboApiProperties();
        properties.setScheduleCacheTtl(Duration.ofMinutes(10));
        properties.setRecordCacheTtl(Duration.ofSeconds(30));
        properties.setLiveGameStateCacheTtl(Duration.ofSeconds(15));
        return properties;
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
