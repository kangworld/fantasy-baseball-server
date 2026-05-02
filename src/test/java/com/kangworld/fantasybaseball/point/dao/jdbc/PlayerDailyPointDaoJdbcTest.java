package com.kangworld.fantasybaseball.point.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.model.PlayerDailyPoint;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from player_daily_point", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlayerDailyPointDaoJdbcTest {

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
