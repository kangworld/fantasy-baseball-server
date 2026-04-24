package com.kangworld.fantasybaseball.point.model;

import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FantasyPointRule {

    private PlayerType playerType;
    private PointRecordCode recordCode;
    private int point;
    private String description;
    private boolean active;

}
