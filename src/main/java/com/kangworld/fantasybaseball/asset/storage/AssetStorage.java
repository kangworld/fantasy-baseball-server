package com.kangworld.fantasybaseball.asset.storage;

import java.util.Optional;

public interface AssetStorage {

    Optional<byte[]> findBytes(String assetPath);

    void upload(String assetPath, AssetFile assetFile);

}
