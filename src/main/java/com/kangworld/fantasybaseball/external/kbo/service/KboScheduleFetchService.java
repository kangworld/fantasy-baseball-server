package com.kangworld.fantasybaseball.external.kbo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.kangworld.fantasybaseball.external.kbo.config.KboApiProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboScheduleFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;

@Service
public class KboScheduleFetchService {

    private final KboScheduleFetcher scheduleFetcher;
    private final LoadingCache<LocalDate, List<KboScheduleGame>> scheduleCache;

    public KboScheduleFetchService(KboScheduleFetcher scheduleFetcher, KboApiProperties properties, Ticker ticker) {
        this.scheduleFetcher = scheduleFetcher;
        this.scheduleCache = Caffeine.newBuilder()
                // 일정은 자주 바뀌지 않아서 조회 시점에 오래된 값만 새로고침하는 쪽으로 맞춘다.
                .refreshAfterWrite(properties.getScheduleCacheTtl().toMillis(), TimeUnit.MILLISECONDS)
                // 날짜 캐시는 최근 조회 범위만 유지해도 충분해서 상한을 작게 둔다.
                .maximumSize(10)
                .ticker(ticker)
                .build(this::loadByDate);
    }

    public List<KboScheduleGame> fetch(LocalDate date) {
        return scheduleCache.get(date);
    }

    public List<KboScheduleGame> findRecordTargetGames(LocalDate date) {
        // 기록 수집은 경기 중, 종료, 서스펜디드만 대상으로 잡아 불필요한 record 호출을 줄인다.
        return fetch(date).stream()
                .filter(game -> game.getStatus().supportsRecordSync())
                .toList();
    }

    public List<KboScheduleGame> findLiveGames(LocalDate date) {
        // polling은 진행 중 경기만 대상으로 잡아 15초 주기 호출 범위를 최소화.
        return fetch(date).stream()
                .filter(game -> game.getStatus().isLive())
                .toList();
    }

    private List<KboScheduleGame> loadByDate(LocalDate date) {
        // 일정 API는 날짜 단위 조회라서 캐시 key도 날짜 하나로 고정.
        return List.copyOf(scheduleFetcher.fetch(date, date));
    }

}
