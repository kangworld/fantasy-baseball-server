package com.kangworld.fantasybaseball.rank.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.model.PlayerDailyPoint;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardBreakdownResponse.Game;
import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;
import com.kangworld.fantasybaseball.support.MySqlContainerIntegrationTest;

@MySqlContainerIntegrationTest
@Transactional
class ScoreboardBreakdownIntegrationTest {

    @Autowired private ScoreboardBreakdownService service;
    @Autowired private RosterDao rosters;
    @Autowired private RosterPlayerDao rosterPlayers;
    @Autowired private PlayerDailyPointDao points;

    /** 경기일 로스터의 슬롯 순서대로 선수를 반환한다. 더블헤더 두 경기의 점수와 기록이 없는 선수도 응답에 포함한다. */
    @Test
    void showsBothGamesAndAnUnusedSlotUsingTheGameDateRoster() {
        LocalDate gameDate = LocalDate.of(2026, 8, 3);
        LocalDate settlementDate = gameDate.plusDays(1);
        LocalDateTime now = settlementDate.atTime(2, 0);
        long rosterId = rosters.insert(Roster.builder().memberId(82).dateId(DateIds.from(gameDate))
                .createdAt(now).updatedAt(now).build());
        rosterPlayers.insertAll(List.of(rosterPlayer(rosterId, 12, 1, now), rosterPlayer(rosterId, 13, 2, now)));
        points.upsertAll(List.of(
                point(settlementDate, "20260803LGHH1", 12, 3, now),
                point(settlementDate, "20260803LGHH2", 12, -8, now),
                point(settlementDate, "20260803LGHH1", 14, 99, now),
                point(gameDate, "20260802LGHH0", 12, 99, now)));

        var result = service.find(82, settlementDate, 0).orElseThrow();

        assertThat(result.gameDate()).isEqualTo(gameDate);
        assertThat(result.rawPoint()).isEqualTo(-5);
        assertThat(result.rankedPoint()).isZero();
        assertThat(result.players()).hasSize(2);
        assertThat(result.players().get(0).point()).isEqualTo(-5);
        assertThat(result.players().get(0).games())
                .containsExactly(new Game("20260803LGHH1", 3), new Game("20260803LGHH2", -8));
        assertThat(result.players().get(1).point()).isZero();
        assertThat(result.players().get(1).games()).isEmpty();
        assertThat(service.find(82, settlementDate, 100)).isEmpty();
    }

    private RosterPlayer rosterPlayer(long rosterId, long playerId, int order, LocalDateTime now) {
        return RosterPlayer.builder().rosterId(rosterId).playerId(playerId).rosterPosition(PlayerPosition.P)
                .positionOrder(order).createdAt(now).updatedAt(now).build();
    }

    private PlayerDailyPoint point(LocalDate date, String game, long player, int point, LocalDateTime now) {
        return PlayerDailyPoint.builder().dateId(DateIds.from(date)).gameId(game).playerId(player).point(point)
                .createdAt(now).updatedAt(now).build();
    }
}
