package com.kangworld.fantasybaseball.point.model;

public record CalculatedPlayerPoint(
        String gameId,
        long playerId,
        int point) {
}
