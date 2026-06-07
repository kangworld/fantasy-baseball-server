package com.kangworld.fantasybaseball.external.kbo.fetcher;

import java.util.Optional;

import com.kangworld.fantasybaseball.external.kbo.model.KboPlayerProfile;

public interface KboPlayerProfileFetcher {

    Optional<KboPlayerProfile> fetch(int season, long playerId);

}
