package com.kangworld.fantasybaseball.roster.service.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RosterResponse {

    private long rosterId;
    private long version;
    private LocalDate date;
    private List<RosterPlayerResponse> players;

}
