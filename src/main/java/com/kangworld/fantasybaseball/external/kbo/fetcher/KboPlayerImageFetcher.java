package com.kangworld.fantasybaseball.external.kbo.fetcher;

import java.util.Optional;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;

public interface KboPlayerImageFetcher {

    Optional<AssetFile> fetch(long playerId, int season);

}
