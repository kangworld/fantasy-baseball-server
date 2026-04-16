package com.kangworld.fantasybaseball.external.kbo.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.kangworld.fantasybaseball.external.kbo.config.KboApiProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboLiveGameStateFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboLiveGameState;

@Service
public class KboLiveGameStateFetchService {

    private final LoadingCache<String, KboLiveGameState> liveGameStateCache;

    public KboLiveGameStateFetchService(KboLiveGameStateFetcher liveGameStateFetcher, KboApiProperties properties, Ticker ticker) {
        this.liveGameStateCache = Caffeine.newBuilder()
                // 실시간 상태는 짧게 stale 처리하고 다음 조회에서 새 값을 다시 읽게 둔다.
                .refreshAfterWrite(properties.getLiveGameStateCacheTtl().toMillis(), TimeUnit.MILLISECONDS)
                // 라이브 상태는 동시에 열려 있는 경기 수만큼만 잡으면 충분하다.
                .maximumSize(10)
                .ticker(ticker)
                .build(liveGameStateFetcher::fetch);
    }

    public KboLiveGameState fetch(String gameId) {
        return liveGameStateCache.get(gameId);
    }

}
