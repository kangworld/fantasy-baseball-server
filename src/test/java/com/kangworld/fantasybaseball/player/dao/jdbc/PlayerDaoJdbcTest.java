package com.kangworld.fantasybaseball.player.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerSearchCondition;
import com.kangworld.fantasybaseball.player.model.PlayerSummary;
import com.kangworld.fantasybaseball.player.model.PlayerType;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "delete from player_profile",
        "delete from player"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlayerDaoJdbcTest {

    @Autowired
    private PlayerDao playerDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findSummaries는_당일_profile과_ACTIVE_선수만_조회한다() {
        // given
        int todayDateId = DateIds.from(LocalDate.now());
        int yesterdayDateId = DateIds.from(LocalDate.now().minusDays(1));
        insertPlayer(76290, "김현수", "BATTER", "DH", "ACTIVE");
        insertPlayer(12345, "삭제선수", "BATTER", "LF", "INACTIVE");
        insertProfile(todayDateId, 76290, "LG", "DH", 3, 12, 8);
        insertProfile(yesterdayDateId, 76290, "LG", "DH", 3, 99, 20);
        insertProfile(todayDateId, 12345, "LG", "LF", 1, 30, 10);

        PlayerSearchCondition condition = PlayerSearchCondition.builder()
                .dateId(todayDateId)
                .build();

        // when
        List<PlayerSummary> summaries = playerDao.findSummaries(condition);

        // then
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getPlayerId()).isEqualTo(76290);
        assertThat(summaries.get(0).getPoint()).isEqualTo(12);
    }

    @Test
    void findSummaries는_타입_포지션_키워드로_필터링한다() {
        // given
        int todayDateId = DateIds.from(LocalDate.now());
        insertPlayer(76290, "김현수", "BATTER", "DH", "ACTIVE");
        insertPlayer(78548, "장성우", "BATTER", "C", "ACTIVE");
        insertPlayer(63492, "김광현", "PITCHER", "P", "ACTIVE");
        insertProfile(todayDateId, 76290, "LG", "DH", 3, 12, 8);
        insertProfile(todayDateId, 78548, "KT", "C", 2, 20, 7);
        insertProfile(todayDateId, 63492, "SSG", "P", 4, 25, 11);

        PlayerSearchCondition condition = PlayerSearchCondition.builder()
                .dateId(todayDateId)
                .playerType(PlayerType.BATTER)
                .positionCode(PlayerPosition.DH)
                .keyword("김")
                .build();

        // when
        List<PlayerSummary> summaries = playerDao.findSummaries(condition);

        // then
        assertThat(summaries).extracting(PlayerSummary::getPlayerId)
                .containsExactly(76290L);
    }

    private void insertPlayer(long playerId, String name, String type, String positionCode, String status) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);

        jdbcTemplate.update("""
                insert into player (player_id, player_name, player_type, position_code, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """, playerId, name, type, positionCode, status, now, now);
    }

    private void insertProfile(int dateId, long playerId, String teamCode, String pos, int cost, int point, int avgPoint) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);

        jdbcTemplate.update("""
                insert into player_profile (date_id, player_id, team_code, pos, cost, point, avg_point, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, dateId, playerId, teamCode, pos, cost, point, avgPoint, now, now);
    }

}
