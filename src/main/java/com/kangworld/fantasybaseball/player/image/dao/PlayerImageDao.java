package com.kangworld.fantasybaseball.player.image.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kangworld.fantasybaseball.player.image.model.PlayerImage;
import com.kangworld.fantasybaseball.player.image.model.PlayerImageRefreshTarget;

public interface PlayerImageDao {

    Map<Long, PlayerImage> findByPlayerIds(Set<Long> playerIds);

    List<PlayerImageRefreshTarget> findRefreshTargets(LocalDateTime teamUpdatedFrom);

    void upsert(PlayerImage playerImage);

}
