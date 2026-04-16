package com.kangworld.fantasybaseball.external.kbo.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboLiveGameState {

    private String gameId;
    private KboGameStatus status;
    private String statusInfo;
    private String currentInning;
    private int homeTeamScore;
    private int awayTeamScore;
    private String homeTeamCode;
    private String awayTeamCode;

}
