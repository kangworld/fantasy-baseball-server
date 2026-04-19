package com.kangworld.fantasybaseball.player.service.dto;

import com.kangworld.fantasybaseball.player.model.PlayerSummary;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerSummaryResponse {

    private long playerId;
    private String playerName;
    private PlayerType playerType;
    private PlayerPosition positionCode;
    private int cost;
    private int point;
    private int avgPoint;
    private String teamCode;
    private String pos;

    public static PlayerSummaryResponse from(PlayerSummary summary) {
        return PlayerSummaryResponse.builder()
                .playerId(summary.getPlayerId())
                .playerName(summary.getPlayerName())
                .playerType(summary.getPlayerType())
                .positionCode(summary.getPositionCode())
                .cost(summary.getCost())
                .point(summary.getPoint())
                .avgPoint(summary.getAvgPoint())
                .teamCode(summary.getTeamCode())
                .pos(summary.getPos())
                .build();
    }

}
