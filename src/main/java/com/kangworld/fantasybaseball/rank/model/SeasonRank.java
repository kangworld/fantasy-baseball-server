package com.kangworld.fantasybaseball.rank.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeasonRank {

    private int dateId;
    private int rankNo;
    private long memberId;
    private int point;
    private int dailyPoint;
    private int displayRankNo;
    private int previousRankNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
