package com.kangworld.fantasybaseball.player.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerProfile {

    private int dateId;
    private long playerId;
    private int cost;
    private int point;
    private int avgPoint;
    private int pointGameCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
