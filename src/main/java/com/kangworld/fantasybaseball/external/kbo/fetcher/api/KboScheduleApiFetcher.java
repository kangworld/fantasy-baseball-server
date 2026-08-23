package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboScheduleFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;

import lombok.Getter;

@Component
public class KboScheduleApiFetcher implements KboScheduleFetcher {

    private static final String KBO_CATEGORY = "kbo";

    private final RestClient kboRestClient;
    private final KboGameStatusMapper gameStatusMapper;

    public KboScheduleApiFetcher(
            @Qualifier("kboRestClient") RestClient kboRestClient,
            KboGameStatusMapper gameStatusMapper) {
        this.kboRestClient = kboRestClient;
        this.gameStatusMapper = gameStatusMapper;
    }

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
            throw new IllegalStateException("KBO 경기 일정 응답이 누락되었습니다. date=" + fromDate);
        }

        // 일정 API는 KBO 외의 부가 경기 목록도 섞여서 내려와서 categoryId로 한 번 더 필터링.
        return response.result.games.stream()
                .filter(game -> KBO_CATEGORY.equalsIgnoreCase(game.categoryId))
                .map(this::toScheduleGame)
                .toList();
    }

    private KboScheduleGame toScheduleGame(ScheduleGameResponse game) {
        if (!StringUtils.hasText(game.gameId) || !StringUtils.hasText(game.gameDateTime)
                || !StringUtils.hasText(game.homeTeamCode) || !StringUtils.hasText(game.awayTeamCode)) {
            throw new IllegalStateException("KBO 경기 일정의 필수 항목이 누락되었습니다.");
        }
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
