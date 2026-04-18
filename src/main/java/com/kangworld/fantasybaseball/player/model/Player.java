package com.kangworld.fantasybaseball.player.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Player {

    private long playerId;
    private String playerName;
    private PlayerType playerType;
    private String positionCode;
    private PlayerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
