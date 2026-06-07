package com.kangworld.fantasybaseball.player.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerSummary {

    private long id;
    private String name;
    private PlayerType type;
    private PlayerPosition position;
    private int cost;
    private int point;
    private int avgPoint;
    private String teamCode;
    private LocalDateTime teamUpdatedAt;
    private String imgPath;

}
