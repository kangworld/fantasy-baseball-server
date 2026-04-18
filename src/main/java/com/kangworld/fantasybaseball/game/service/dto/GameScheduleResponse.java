package com.kangworld.fantasybaseball.game.service.dto;

import java.time.LocalDateTime;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameScheduleResponse {

    private String gameId;
    private LocalDateTime startAt;
    private KboGameStatus status;
    private String statusCode;
    private String statusInfo;
    private String homeTeamCode;
    private String homeTeamName;
    private String awayTeamCode;
    private String awayTeamName;

    public static GameScheduleResponse from(KboScheduleGame game) {
        return GameScheduleResponse.builder()
                .gameId(game.getGameId())
                .startAt(game.getStartAt())
                .status(game.getStatus())
                .statusCode(game.getStatusCode())
                .statusInfo(game.getStatusInfo())
                .homeTeamCode(game.getHomeTeamCode())
                .homeTeamName(game.getHomeTeamName())
                .awayTeamCode(game.getAwayTeamCode())
                .awayTeamName(game.getAwayTeamName())
                .build();
    }

}
