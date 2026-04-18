package com.kangworld.fantasybaseball.player.dao;

import java.util.List;

import com.kangworld.fantasybaseball.player.model.PlayerSearchCondition;
import com.kangworld.fantasybaseball.player.model.PlayerSummary;

public interface PlayerDao {

    List<PlayerSummary> findSummaries(PlayerSearchCondition condition);

}
