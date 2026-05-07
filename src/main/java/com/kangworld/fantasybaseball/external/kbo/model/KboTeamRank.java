package com.kangworld.fantasybaseball.external.kbo.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboTeamRank {

    private int ranking;
    private String teamCode;
    private String teamName;
    private String teamShortName;
    private int gameCount;
    private int winGameCount;
    private int drawnGameCount;
    private int loseGameCount;
    private double winningRate;
    private double gameBehind;
    private String continuousGameResult;
    private String lastFiveGames;
    private String nextScheduleGameId;
    private String opposingTeamName;

}
