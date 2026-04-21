package com.kangworld.fantasybaseball.roster.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RosterPlayerRequest {

    @NotNull
    @Positive
    private Integer slotNo;

    @NotNull
    @Positive
    private Long playerId;

}
