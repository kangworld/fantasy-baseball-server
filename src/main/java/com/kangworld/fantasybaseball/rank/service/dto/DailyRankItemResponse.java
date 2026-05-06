package com.kangworld.fantasybaseball.rank.service.dto;

import com.kangworld.fantasybaseball.rank.dao.view.DailyRankView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyRankItemResponse {

    private int rankNo;
    private int displayRankNo;
    private int previousRankNo;
    private long memberId;
    private String displayName;
    private int point;

    public static DailyRankItemResponse from(DailyRankView rank) {
        return DailyRankItemResponse.builder()
                .rankNo(rank.getRankNo())
                .displayRankNo(rank.getDisplayRankNo())
                .previousRankNo(rank.getPreviousRankNo())
                .memberId(rank.getMemberId())
                .displayName(rank.getDisplayName())
                .point(rank.getPoint())
                .build();
    }

}
