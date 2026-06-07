package com.kangworld.fantasybaseball.player.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Player {

    private long id;
    private String name;
    private PlayerType type;
    private PlayerPosition position;
    private String teamCode;
    private PlayerStatus status;
    private LocalDateTime teamUpdatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
