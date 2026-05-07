package com.kangworld.fantasybaseball.external.kbo.fetcher;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kangworld.fantasybaseball.external.kbo.model.KboTeamRank;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KboTeamRankApiFetcher implements KboTeamRankFetcher {

    private static final String REGULAR_SEASON = "REGULAR_SEASON";

    private final RestClient kboRestClient;

    @Override
    public List<KboTeamRank> fetch(int season) {
        TeamRankResponse response = kboRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/statistics/categories/kbo/seasons/{season}/teams")
                        .queryParam("gameType", REGULAR_SEASON)
                        .build(season))
                .retrieve()
                .body(TeamRankResponse.class);

        if (response == null || response.result == null || response.result.seasonTeamStats == null)
            return List.of();

        return response.result.seasonTeamStats.stream()
                .map(this::toTeamRank)
                .toList();
    }

    private KboTeamRank toTeamRank(SeasonTeamStatResponse team) {
        return KboTeamRank.builder()
                .ranking(team.ranking)
                .teamCode(team.teamId)
                .teamName(team.teamName)
                .teamShortName(team.teamShortName)
                .gameCount(team.gameCount)
                .winGameCount(team.winGameCount)
                .drawnGameCount(team.drawnGameCount)
                .loseGameCount(team.loseGameCount)
                .winningRate(team.wra)
                .gameBehind(team.gameBehind)
                .continuousGameResult(team.continuousGameResult)
                .lastFiveGames(team.lastFiveGames)
                .nextScheduleGameId(team.nextScheduleGameId)
                .opposingTeamName(team.opposingTeamName)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class TeamRankResponse {
        private TeamRankResultResponse result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class TeamRankResultResponse {
        private List<SeasonTeamStatResponse> seasonTeamStats;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class SeasonTeamStatResponse {
        private int ranking;
        private String teamId;
        private String teamName;
        private String teamShortName;
        private int gameCount;
        private int winGameCount;
        private int drawnGameCount;
        private int loseGameCount;
        private double wra;
        private double gameBehind;
        private String continuousGameResult;
        private String lastFiveGames;
        private String nextScheduleGameId;
        private String opposingTeamName;
    }

}
