package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboLiveGameStateFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.external.kbo.model.KboLiveGameState;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KboLiveGameStateApiFetcher implements KboLiveGameStateFetcher {

    private final RestClient kboRestClient;
    private final KboGameStatusMapper gameStatusMapper;

    @Override
    public KboLiveGameState fetch(String gameId) {
        PollingResponse response = kboRestClient.get()
                .uri("/schedule/games/{gameId}/game-polling", gameId)
                .retrieve()
                .body(PollingResponse.class);

        if (response == null || response.result == null || response.result.game == null) {
            return KboLiveGameState.builder()
                    .gameId(gameId)
                    .status(KboGameStatus.UNKNOWN)
                    .build();
        }

        PollingGameResponse game = response.result.game;

        return KboLiveGameState.builder()
                .gameId(game.gameId)
                .status(gameStatusMapper.map(game.statusCode, game.cancel, game.suspended))
                .statusCode(game.statusCode)
                .statusInfo(game.statusInfo)
                .currentInning(game.currentInning)
                .homeTeamScore(game.homeTeamScore)
                .awayTeamScore(game.awayTeamScore)
                .homeTeamCode(game.homeTeamCode)
                .awayTeamCode(game.awayTeamCode)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class PollingResponse {
        private PollingResult result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class PollingResult {
        private PollingGameResponse game;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class PollingGameResponse {
        private String gameId;
        private String statusCode;
        private String statusInfo;
        private String currentInning;
        private int homeTeamScore;
        private int awayTeamScore;
        private String homeTeamCode;
        private String awayTeamCode;
        private boolean cancel;
        private boolean suspended;
    }

}
