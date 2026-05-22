package com.kangworld.fantasybaseball.player.image.dao;

import java.util.Map;
import java.util.Set;

import com.kangworld.fantasybaseball.player.image.model.PlayerImage;

public interface PlayerImageDao {

    Map<Long, PlayerImage> findByPlayerIds(Set<Long> playerIds);

    void upsert(PlayerImage playerImage);

}
