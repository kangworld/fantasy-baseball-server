package com.kangworld.fantasybaseball.point.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.model.PlayerDailyPoint;
import com.kangworld.fantasybaseball.support.MySqlContainerIntegrationTest;

@MySqlContainerIntegrationTest
@Sql(statements = "delete from player_daily_point", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlayerDailyPointDaoJdbcIntegrationTest {

    @Autowired
    private PlayerDailyPointDao playerDailyPointDao;

    @Test
    void upsertAll은_동일_날짜_경기_선수의_일간_포인트를_갱신한다() {
        // given
        PlayerDailyPoint first = point(20260506, "game-1", 72443, 10);
        PlayerDailyPoint updated = point(20260506, "game-1", 72443, 15);
        PlayerDailyPoint pitcher = point(20260506, "game-1", 65320, 20);

        // when
        playerDailyPointDao.upsertAll(List.of(first));
        playerDailyPointDao.upsertAll(List.of(updated, pitcher));

        // then
        Map<Long, Integer> pointByPlayerId = playerDailyPointDao.findPointByPlayerIds(
                20260506,
                Set.of(72443L, 65320L, 1L)
        );

        assertThat(pointByPlayerId).containsEntry(72443L, 15);
        assertThat(pointByPlayerId).containsEntry(65320L, 20);
    }

    @Test
    void findTotalPointByPlayerIdsBetweenDateIds는_기간_내_선수별_포인트를_합산한다() {
        // given
        playerDailyPointDao.upsertAll(List.of(
                point(20260502, "game-1", 100L, 10),
                point(20260503, "game-2", 100L, 15),
                point(20260503, "game-2", 200L, 20),
                point(20260517, "game-3", 100L, 100)
        ));

        // when
        Map<Long, Integer> pointByPlayerId = playerDailyPointDao.findTotalPointByPlayerIdsBetweenDateIds(
                20260502,
                20260516,
                Set.of(100L, 200L, 300L)
        );

        // then
        assertThat(pointByPlayerId).containsEntry(100L, 25);
        assertThat(pointByPlayerId).containsEntry(200L, 20);
        assertThat(pointByPlayerId).doesNotContainKey(300L);
    }

    private PlayerDailyPoint point(int dateId, String gameId, long playerId, int point) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 6, 23, 0);

        return PlayerDailyPoint.builder()
                .dateId(dateId)
                .gameId(gameId)
                .playerId(playerId)
                .point(point)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
