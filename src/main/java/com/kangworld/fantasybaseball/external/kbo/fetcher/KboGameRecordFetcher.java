package com.kangworld.fantasybaseball.external.kbo.fetcher;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;

public interface KboGameRecordFetcher {

    KboGameRecord fetch(String gameId);

}
