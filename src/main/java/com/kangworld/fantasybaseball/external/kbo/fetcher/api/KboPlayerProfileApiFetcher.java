package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboPlayerProfileFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboPlayerProfile;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KboPlayerProfileApiFetcher implements KboPlayerProfileFetcher {

    private final RestClient kboRestClient;

    public KboPlayerProfileApiFetcher(@Qualifier("kboRestClient") RestClient kboRestClient) {
        this.kboRestClient = kboRestClient;
    }

    @Override
    public Optional<KboPlayerProfile> fetch(int season, long playerId) {
        PlayerProfileResponse response = kboRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/statistics/categories/kbo/seasons/{season}/players/{playerId}")
                        .queryParam("gameType", "REGULAR_SEASON")
                        .build(season, playerId))
                .retrieve()
                .body(PlayerProfileResponse.class);

        if (response == null || response.result == null || response.result.player == null) {
            log.warn("KBO player profile is empty. season={}, playerId={}", season, playerId);
            return Optional.empty();
        }

        PlayerResponse player = response.result.player;
        PlayerPosition position = toPlayerPosition(player.position);

        return Optional.of(KboPlayerProfile.builder()
                .playerId(playerId)
                .name(player.name)
                .type(position == PlayerPosition.P ? PlayerType.PITCHER : PlayerType.BATTER)
                .position(position)
                .build());
    }

    private PlayerPosition toPlayerPosition(String position) {
        if (position == null || position.isBlank())
            return PlayerPosition.DH;

        return switch (position) {
            case "PITCHER" -> PlayerPosition.P;
            case "CATCHER" -> PlayerPosition.C;
            case "FIRST_BASE" -> PlayerPosition.B1;
            case "SECOND_BASE" -> PlayerPosition.B2;
            case "THIRD_BASE" -> PlayerPosition.B3;
            case "SHORT_STOP" -> PlayerPosition.SS;
            case "LEFT_FIELDER" -> PlayerPosition.LF;
            case "CENTER_FIELDER" -> PlayerPosition.CF;
            case "RIGHT_FIELDER" -> PlayerPosition.RF;
            default -> PlayerPosition.DH;
        };
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerProfileResponse {
        private PlayerProfileResult result;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerProfileResult {
        private PlayerResponse player;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerResponse {
        private String name;
        private String position;
    }

}
