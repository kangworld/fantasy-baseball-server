package com.kangworld.fantasybaseball.external.kbo.fetcher;

import java.util.List;

import com.kangworld.fantasybaseball.external.kbo.model.KboTeamRank;

public interface KboTeamRankFetcher {

    List<KboTeamRank> fetch(int season);

}
