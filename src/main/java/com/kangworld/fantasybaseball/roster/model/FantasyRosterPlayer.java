package com.kangworld.fantasybaseball.roster.model;

import java.time.LocalDateTime;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FantasyRosterPlayer {

    private long rosterPlayerId;
    private long rosterId;
    private int slotNo;
    private long playerId;
    private PlayerType playerType;
    private PlayerPosition positionCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
