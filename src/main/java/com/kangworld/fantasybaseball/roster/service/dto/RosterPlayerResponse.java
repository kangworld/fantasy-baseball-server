package com.kangworld.fantasybaseball.roster.service.dto;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RosterPlayerResponse {

    private int slotNo;
    private long playerId;
    private String playerName;
    private PlayerType playerType;
    private PlayerPosition positionCode;
    private int cost;
    private int point;
    private int avgPoint;
    private String imgPath;

}
