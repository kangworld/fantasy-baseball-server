package com.kangworld.fantasybaseball.rank.service.dto;

import java.time.LocalDate;
import java.util.List;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;

public record ScoreboardBreakdownResponse(
        LocalDate gameDate,
        int rawPoint,
        int rankedPoint,
        List<Player> players) {

    public record Player(
            long playerId,
            PlayerPosition rosterPosition,
            int positionOrder,
            int point,
            List<Game> games) {
    }

    public record Game(String gameId, int point) {
    }
}
