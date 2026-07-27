package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.rank.model.DailyRank;
import com.kangworld.fantasybaseball.rank.model.SeasonRank;
import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;
import com.kangworld.fantasybaseball.support.MySqlContainerIntegrationTest;

@MySqlContainerIntegrationTest
@Transactional
class RankSettlementServiceIntegrationTest {

    @Autowired
    private RankSettlementService rankSettlementService;

    @Autowired
    private RosterDao rosterDao;

    @Autowired
    private RosterPlayerDao rosterPlayerDao;

    @Autowired
    private PlayerDailyPointDao playerDailyPointDao;

    @Autowired
    private DailyRankDao dailyRankDao;

    @Autowired
    private SeasonRankDao seasonRankDao;

    /** 경기일 로스터와 정산일 로스터를 함께 저장한다. 정산일 랭킹에는 경기일 제출분과 정산일에 저장한 점수만 반영되어야 한다. */
    @Test
    void settle은_경기일_로스터만_정산일_랭킹에_반영한다() {
        LocalDate gameDate = LocalDate.of(2026, 5, 18);
        LocalDate settlementDate = gameDate.plusDays(1);
        int gameDateId = DateIds.from(gameDate);
        int settlementDateId = DateIds.from(settlementDate);
        LocalDateTime settlementTime = settlementDate.atTime(2, 0);
        saveRoster(10L, 101L, gameDateId, gameDate.atTime(12, 0));
        saveRoster(20L, 202L, settlementDateId, settlementDate.atTime(12, 0));
        playerDailyPointDao.upsertAll(List.of(
                PlayerDailyPoint.builder()
                        .dateId(settlementDateId)
                        .gameId("20260518LGHH0")
                        .playerId(101L)
                        .point(37)
                        .createdAt(settlementTime)
                        .updatedAt(settlementTime)
                        .build()
        ));

        rankSettlementService.settle(settlementDate);

        assertThat(dailyRankDao.findByDateId(settlementDateId))
                .extracting(DailyRank::getMemberId, DailyRank::getPoint)
                .containsExactly(tuple(10L, 37));
        assertThat(seasonRankDao.findByDateId(settlementDateId))
                .extracting(SeasonRank::getMemberId, SeasonRank::getDailyPoint)
                .containsExactly(tuple(10L, 37));
    }

    private void saveRoster(long memberId, long playerId, int dateId, LocalDateTime submittedAt) {
        long rosterId = rosterDao.insert(Roster.builder()
                .memberId(memberId)
                .dateId(dateId)
                .createdAt(submittedAt)
                .updatedAt(submittedAt)
                .build());
        rosterPlayerDao.insertAll(List.of(
                RosterPlayer.builder()
                        .rosterId(rosterId)
                        .rosterPosition(PlayerPosition.P)
                        .positionOrder(1)
                        .playerId(playerId)
                        .createdAt(submittedAt)
                        .updatedAt(submittedAt)
                        .build()
        ));
    }
}
