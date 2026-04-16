package com.kangworld.fantasybaseball.external.kbo.fetcher;

import com.kangworld.fantasybaseball.external.kbo.model.KboLiveGameState;

public interface KboLiveGameStateFetcher {

    KboLiveGameState fetch(String gameId);

}
