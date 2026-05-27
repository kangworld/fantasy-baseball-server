package com.kangworld.fantasybaseball.roster.model;

import java.time.LocalDateTime;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RosterPlayer {

    private long rosterPlayerId;
    private long rosterId;
    private PlayerPosition rosterPosition;
    private int positionOrder;
    private long playerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
