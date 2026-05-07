package com.kangworld.fantasybaseball.external.kbo.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.kangworld.fantasybaseball.external.kbo.config.KboApiProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboTeamRankFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboTeamRank;

@Service
public class KboTeamRankFetchService {

    private final LoadingCache<Integer, List<KboTeamRank>> teamRankCache;

    public KboTeamRankFetchService(KboTeamRankFetcher teamRankFetcher, KboApiProperties properties, Ticker ticker) {
        this.teamRankCache = Caffeine.newBuilder()
                // 팀 순위표는 모든 사용자가 같은 시즌 데이터를 보므로 짧은 시즌 단위 캐시만 둔다.
                .refreshAfterWrite(properties.getTeamRankCacheTtl().toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(3)
                .ticker(ticker)
                .build(season -> List.copyOf(teamRankFetcher.fetch(season)));
    }

    public List<KboTeamRank> fetch(int season) {
        return teamRankCache.get(season);
    }

}
