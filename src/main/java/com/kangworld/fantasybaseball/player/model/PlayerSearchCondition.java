package com.kangworld.fantasybaseball.player.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerSearchCondition {

    private int dateId;
    private PlayerType playerType;
    private String positionCode;
    private String keyword;

}
