package com.kangworld.fantasybaseball.player.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerSummary {

    private long playerId;
    private String playerName;
    private PlayerType playerType;
    private PlayerPosition positionCode;
    private int cost;
    private int point;
    private int avgPoint;
    private String teamCode;
    private String pos;
    private String imgPath;

}
