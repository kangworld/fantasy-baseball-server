package com.kangworld.fantasybaseball.rank.service.dto;

import com.kangworld.fantasybaseball.rank.dao.view.SeasonRankView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreboardSeasonResponse {

    private int point;
    private int dailyPoint;
    private int rankNo;
    private int displayRankNo;
    private int previousRankNo;

    public static ScoreboardSeasonResponse from(SeasonRankView rank) {
        return ScoreboardSeasonResponse.builder()
                .point(rank.getPoint())
                .dailyPoint(rank.getDailyPoint())
                .rankNo(rank.getRankNo())
                .displayRankNo(rank.getDisplayRankNo())
                .previousRankNo(rank.getPreviousRankNo())
                .build();
    }

    public static ScoreboardSeasonResponse zero() {
        return ScoreboardSeasonResponse.builder()
                .point(0)
                .dailyPoint(0)
                .rankNo(0)
                .displayRankNo(0)
                .previousRankNo(0)
                .build();
    }

}
