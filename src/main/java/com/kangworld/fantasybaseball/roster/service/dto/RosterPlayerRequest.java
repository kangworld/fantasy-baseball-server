package com.kangworld.fantasybaseball.roster.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RosterPlayerRequest {

    @NotNull
    private PlayerPosition rosterPosition;

    @NotNull
    @Positive
    private Integer positionOrder;

    @NotNull
    @Positive
    private Long playerId;

    public boolean isPitcher() {
        return rosterPosition == PlayerPosition.P;
    }

}
