package com.kangworld.fantasybaseball.rank.service.dto;

import com.kangworld.fantasybaseball.rank.dao.view.SeasonRankView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeasonRankItemResponse {

    private int rankNo;
    private int displayRankNo;
    private int previousRankNo;
    private long memberId;
    private String displayName;
    private int point;
    private int dailyPoint;

    public static SeasonRankItemResponse from(SeasonRankView rank) {
        return SeasonRankItemResponse.builder()
                .rankNo(rank.getRankNo())
                .displayRankNo(rank.getDisplayRankNo())
                .previousRankNo(rank.getPreviousRankNo())
                .memberId(rank.getMemberId())
                .displayName(rank.getDisplayName())
                .point(rank.getPoint())
                .dailyPoint(rank.getDailyPoint())
                .build();
    }

}
