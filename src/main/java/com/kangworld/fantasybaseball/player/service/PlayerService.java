package com.kangworld.fantasybaseball.player.service;

import java.util.List;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.player.service.dto.PlayerSummaryResponse;

public interface PlayerService {

    List<PlayerSummaryResponse> findPlayers(PlayerType playerType, PlayerPosition positionCode, String keyword);

}
