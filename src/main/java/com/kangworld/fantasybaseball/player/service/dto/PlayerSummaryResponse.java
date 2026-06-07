package com.kangworld.fantasybaseball.player.service.dto;

import com.kangworld.fantasybaseball.player.model.PlayerSummary;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerSummaryResponse {

    private long id;
    private String name;
    private PlayerType type;
    private PlayerPosition position;
    private int cost;
    private int point;
    private int avgPoint;
    private String teamCode;
    private String imgPath;

    public static PlayerSummaryResponse from(PlayerSummary summary) {
        return PlayerSummaryResponse.builder()
                .id(summary.getId())
                .name(summary.getName())
                .type(summary.getType())
                .position(summary.getPosition())
                .cost(summary.getCost())
                .point(summary.getPoint())
                .avgPoint(summary.getAvgPoint())
                .teamCode(summary.getTeamCode())
                .imgPath(summary.getImgPath())
                .build();
    }

}
