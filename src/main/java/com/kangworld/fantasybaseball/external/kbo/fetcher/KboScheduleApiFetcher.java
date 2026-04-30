package com.kangworld.fantasybaseball.external.kbo.fetcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KboScheduleApiFetcher implements KboScheduleFetcher {

    private static final String KBO_CATEGORY = "kbo";

    private final RestClient kboRestClient;
    private final KboGameStatusMapper gameStatusMapper;

    @Override
    public List<KboScheduleGame> fetch(LocalDate fromDate, LocalDate toDate) {
        ScheduleResponse response = kboRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/schedule/games")
                        .queryParam("fields", "basic,schedule,baseball,manualRelayUrl")
                        .queryParam("upperCategoryId", "kbaseball")
                        .queryParam("fromDate", fromDate.toString())
                        .queryParam("toDate", toDate.toString())
                        .queryParam("size", 500)
                        .build())
                .retrieve()
                .body(ScheduleResponse.class);

        if (response == null || response.result == null || response.result.games == null) {
            return List.of();
        }

        // 일정 API는 KBO 외의 부가 경기 목록도 섞여서 내려와서 categoryId로 한 번 더 필터링.
        return response.result.games.stream()
                .filter(game -> KBO_CATEGORY.equalsIgnoreCase(game.categoryId))
                .map(this::toScheduleGame)
                .toList();
    }

    private KboScheduleGame toScheduleGame(ScheduleGameResponse game) {
        return KboScheduleGame.builder()
                .gameId(game.gameId)
                .startAt(LocalDateTime.parse(game.gameDateTime))
                .homeTeamCode(game.homeTeamCode)
                .homeTeamName(game.homeTeamName)
                .awayTeamCode(game.awayTeamCode)
                .awayTeamName(game.awayTeamName)
                .statusCode(game.statusCode)
                .statusInfo(game.statusInfo)
                .status(gameStatusMapper.map(game.statusCode, game.cancel, game.suspended))
                .canceled(game.cancel)
                .suspended(game.suspended)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class ScheduleResponse {
        private ScheduleResult result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class ScheduleResult {
        private List<ScheduleGameResponse> games;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    private static class ScheduleGameResponse {
        private String gameId;
        private String categoryId;
        private String gameDateTime;
        private String homeTeamCode;
        private String homeTeamName;
        private String awayTeamCode;
        private String awayTeamName;
        private String statusCode;
        private String statusInfo;
        private boolean cancel;
        private boolean suspended;
    }

}
