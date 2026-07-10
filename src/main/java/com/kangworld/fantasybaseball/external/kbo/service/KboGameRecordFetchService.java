package com.kangworld.fantasybaseball.external.kbo.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.kangworld.fantasybaseball.external.kbo.config.KboApiProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboGameRecordFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;

@Service
public class KboGameRecordFetchService {

    private final LoadingCache<String, KboGameRecord> recordCache;

    public KboGameRecordFetchService(KboGameRecordFetcher gameRecordFetcher, KboApiProperties properties, Ticker ticker) {
        this.recordCache = Caffeine.newBuilder()
                // 경기 기록은 경기 중에도 바뀌어서 짧게 새로고침되게 둔다.
                .refreshAfterWrite(properties.recordCacheTtl().toMillis(), TimeUnit.MILLISECONDS)
                // 경기 단위 캐시는 당일 활성 경기 수를 넘지 않게 상한을 둔다.
                .maximumSize(10)
                .ticker(ticker)
                .build(gameRecordFetcher::fetch);
    }

    public KboGameRecord fetch(String gameId) {
        return recordCache.get(gameId);
    }

}
