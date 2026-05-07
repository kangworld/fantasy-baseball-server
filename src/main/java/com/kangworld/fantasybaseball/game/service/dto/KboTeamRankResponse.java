package com.kangworld.fantasybaseball.game.service.dto;

import com.kangworld.fantasybaseball.external.kbo.model.KboTeamRank;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboTeamRankResponse {

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

    public static KboTeamRankResponse from(KboTeamRank rank) {
        return KboTeamRankResponse.builder()
                .ranking(rank.getRanking())
                .teamCode(rank.getTeamCode())
                .teamName(rank.getTeamName())
                .teamShortName(rank.getTeamShortName())
                .gameCount(rank.getGameCount())
                .winGameCount(rank.getWinGameCount())
                .drawnGameCount(rank.getDrawnGameCount())
                .loseGameCount(rank.getLoseGameCount())
                .winningRate(rank.getWinningRate())
                .gameBehind(rank.getGameBehind())
                .continuousGameResult(rank.getContinuousGameResult())
                .lastFiveGames(rank.getLastFiveGames())
                .nextScheduleGameId(rank.getNextScheduleGameId())
                .opposingTeamName(rank.getOpposingTeamName())
                .build();
    }

}
