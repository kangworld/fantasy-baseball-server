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

    public static ScoreboardDailyResponse from(DailyRankView rank) {
        return ScoreboardDailyResponse.builder()
                .point(rank.getPoint())
                .rankNo(rank.getRankNo())
                .displayRankNo(rank.getDisplayRankNo())
                .build();
    }

    public static ScoreboardDailyResponse zero() {
        return ScoreboardDailyResponse.builder()
                .point(0)
                .rankNo(0)
                .displayRankNo(0)
                .build();
    }

}
