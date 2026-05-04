package com.kangworld.fantasybaseball.rank.dao.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeasonRankView {

    private int rankNo;
    private int displayRankNo;
    private int previousRankNo;
    private long memberId;
    private String displayName;
    private int point;
    private int dailyPoint;

}
