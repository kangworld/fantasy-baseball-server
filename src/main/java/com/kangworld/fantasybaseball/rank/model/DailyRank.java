package com.kangworld.fantasybaseball.rank.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyRank {

    private int dateId;
    private int rankNo;
    private long memberId;
    private int point;
    private int displayRankNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
