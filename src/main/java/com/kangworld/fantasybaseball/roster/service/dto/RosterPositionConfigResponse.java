package com.kangworld.fantasybaseball.roster.service.dto;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RosterPositionConfigResponse {

    private PlayerPosition rosterPosition;
    private int count;

}
