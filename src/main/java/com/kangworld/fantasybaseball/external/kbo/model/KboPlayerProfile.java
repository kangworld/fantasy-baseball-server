package com.kangworld.fantasybaseball.external.kbo.model;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboPlayerProfile {

    private long playerId;
    private String name;
    private PlayerType type;
    private PlayerPosition position;

}
