package com.kangworld.fantasybaseball.game.service.dto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboLiveGameState;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameLiveStateResponse {

    private static final Pattern INNING_PATTERN = Pattern.compile("^(\\d+)(?:회)?(초|말)$");

    private String gameId;
    private KboGameStatus status;
    private String statusCode;
    private String statusInfo;
    private Integer inning;
    private Boolean topInning;
    private int homeTeamScore;
    private int awayTeamScore;
    private String homeTeamCode;
    private String awayTeamCode;

    public static GameLiveStateResponse from(KboLiveGameState state) {
        Matcher inningMatcher = INNING_PATTERN.matcher(getInningText(state));
        Integer inning = null;
        Boolean topInning = null;

        if (inningMatcher.matches()) {
            inning = Integer.valueOf(inningMatcher.group(1));
            topInning = "초".equals(inningMatcher.group(2));
        }

        return GameLiveStateResponse.builder()
                .gameId(state.getGameId())
                .status(state.getStatus())
                .statusCode(state.getStatusCode())
                .statusInfo(state.getStatusInfo())
                .inning(inning)
                .topInning(topInning)
                .homeTeamScore(state.getHomeTeamScore())
                .awayTeamScore(state.getAwayTeamScore())
                .homeTeamCode(state.getHomeTeamCode())
                .awayTeamCode(state.getAwayTeamCode())
                .build();
    }

    private static String getInningText(KboLiveGameState state) {
        if (state.getStatus() != KboGameStatus.PLAYING)
            return "";

        if (state.getCurrentInning() != null)
            return state.getCurrentInning().trim();

        return String.valueOf(state.getStatusInfo()).trim();
    }

}
