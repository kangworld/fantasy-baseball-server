package com.kangworld.fantasybaseball.point.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerDailyPoint {

    private int dateId;
    private String gameId;
    private long playerId;
    private int point;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
