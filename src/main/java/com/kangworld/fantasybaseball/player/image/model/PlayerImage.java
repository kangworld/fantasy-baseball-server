package com.kangworld.fantasybaseball.player.image.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerImage {

    private long playerId;
    private String imgPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
