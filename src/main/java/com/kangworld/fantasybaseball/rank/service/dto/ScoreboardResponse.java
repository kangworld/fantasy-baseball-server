package com.kangworld.fantasybaseball.rank.service.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreboardResponse {

    private LocalDate date;
    private ScoreboardDailyResponse daily;
    private ScoreboardSeasonResponse season;

}
