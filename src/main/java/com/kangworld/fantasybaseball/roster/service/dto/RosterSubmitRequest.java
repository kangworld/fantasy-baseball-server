package com.kangworld.fantasybaseball.roster.service.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RosterSubmitRequest {

    @Valid
    @NotEmpty
    private List<RosterPlayerRequest> players;

    @PositiveOrZero
    private Long version;

    public RosterSubmitRequest(List<RosterPlayerRequest> players) {
        this(players, null);
    }

}
