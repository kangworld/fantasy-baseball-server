package com.kangworld.fantasybaseball.rank.service.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.model.PlayerGamePoint;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardBreakdownResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardBreakdownResponse.Game;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardBreakdownResponse.Player;
import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;
import com.kangworld.fantasybaseball.roster.service.RosterLineupRules;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ScoreboardBreakdownService {

    private final RosterDao rosterDao;
    private final RosterPlayerDao rosterPlayerDao;
    private final PlayerDailyPointDao playerDailyPointDao;
    private final RosterLineupRules lineupRules;

    Optional<ScoreboardBreakdownResponse> find(long memberId, LocalDate settlementDate, int rankedPoint) {
        LocalDate gameDate = settlementDate.minusDays(1);

        return rosterDao.findByMemberIdAndDateId(memberId, DateIds.from(gameDate))
                .map(roster -> rosterPlayerDao.findByRosterId(roster.getRosterId()))
                .filter(players -> !players.isEmpty())
                .map(players -> create(gameDate, DateIds.from(settlementDate), rankedPoint, players))
                .filter(breakdown -> Math.max(breakdown.rawPoint(), 0) == rankedPoint);
    }

    private ScoreboardBreakdownResponse create(
            LocalDate gameDate, int settlementDateId, int rankedPoint, List<RosterPlayer> rosterPlayers) {
        Set<Long> playerIds = rosterPlayers.stream().map(RosterPlayer::getPlayerId).collect(Collectors.toSet());
        Map<Long, List<PlayerGamePoint>> pointsByPlayer = playerDailyPointDao
                .findGamePointsByPlayerIds(settlementDateId, playerIds).stream()
                .collect(Collectors.groupingBy(PlayerGamePoint::playerId));
        List<Player> players = rosterPlayers.stream()
                .sorted(Comparator
                        .comparingInt((RosterPlayer player) -> lineupRules.getDisplayOrder(player.getRosterPosition()))
                        .thenComparingInt(RosterPlayer::getPositionOrder))
                .map(player -> toPlayer(player, pointsByPlayer.getOrDefault(player.getPlayerId(), List.of())))
                .toList();
        int rawPoint = players.stream().mapToInt(Player::point).sum();

        return new ScoreboardBreakdownResponse(gameDate, rawPoint, rankedPoint, players);
    }

    private Player toPlayer(RosterPlayer player, List<PlayerGamePoint> points) {
        List<Game> games = points.stream().map(point -> new Game(point.gameId(), point.point())).toList();
        int total = games.stream().mapToInt(Game::point).sum();

        return new Player(player.getPlayerId(), player.getRosterPosition(), player.getPositionOrder(), total, games);
    }
}
