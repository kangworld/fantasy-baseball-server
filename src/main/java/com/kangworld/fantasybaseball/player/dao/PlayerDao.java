package com.kangworld.fantasybaseball.player.dao;

import java.util.List;
import java.util.Set;

import com.kangworld.fantasybaseball.player.model.PlayerSearchCondition;
import com.kangworld.fantasybaseball.player.model.PlayerSummary;
import com.kangworld.fantasybaseball.player.model.Player;

public interface PlayerDao {

    void upsertAll(List<Player> players);

    List<PlayerSummary> findSummaries(PlayerSearchCondition condition);

    List<PlayerSummary> findSummariesByPlayerIds(int dateId, Set<Long> playerIds);

}
