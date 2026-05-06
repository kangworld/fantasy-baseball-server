package com.kangworld.fantasybaseball.rank.service.dto;

import com.kangworld.fantasybaseball.rank.dao.view.DailyRankView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreboardDailyResponse {

    private int point;
    private int rankNo;
    private int displayRankNo;
    private int previousRankNo;

    public static ScoreboardDailyResponse from(DailyRankView rank) {
        return ScoreboardDailyResponse.builder()
                .point(rank.getPoint())
                .rankNo(rank.getRankNo())
                .displayRankNo(rank.getDisplayRankNo())
                .previousRankNo(rank.getPreviousRankNo())
                .build();
    }

    public static ScoreboardDailyResponse empty() {
        return ScoreboardDailyResponse.builder()
                .point(0)
                .rankNo(0)
                .displayRankNo(0)
                .previousRankNo(0)
                .build();
    }

}
