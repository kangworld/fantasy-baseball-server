package com.kangworld.fantasybaseball.point.dao.jdbc;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.model.PlayerDailyPoint;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlayerDailyPointDaoJdbc implements PlayerDailyPointDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void upsertAll(List<PlayerDailyPoint> dailyPoints) {
        // 같은 날짜/경기/선수 점수는 재계산 결과로 덮어쓰기
        if (dailyPoints.isEmpty())
            return;

        String sql = """
                insert into player_daily_point values (
                    :dateId,
                    :gameId,
                    :playerId,
                    :point,
                    :createdAt,
                    :updatedAt
                )
                on duplicate key update
                    point = values(point),
                    updated_at = values(updated_at)
                """;

        MapSqlParameterSource[] parameters = dailyPoints.stream()
                .map(this::toParameterSource)
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, parameters);
    }

    @Override
    public Map<Long, Integer> findPointByPlayerIds(int dateId, Set<Long> playerIds) {
        // 조회 결과에 없는 playerId는 호출자가 0점으로 처리
        if (playerIds.isEmpty())
            return Map.of();

        String sql = """
                select player_id, sum(point) as point
                from player_daily_point
                where date_id = :dateId
                  and player_id in (:playerIds)
                group by player_id
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("dateId", dateId)
                .addValue("playerIds", playerIds);

        return namedParameterJdbcTemplate.query(sql, parameters, (rs, rowNum) ->
                Map.entry(rs.getLong("player_id"), rs.getInt("point"))
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<Long, Integer> findTotalPointByPlayerIdsBetweenDateIds(int fromDateId, int toDateId, Set<Long> playerIds) {
        // 코스트 산정은 최근 구간의 선수별 총점을 쓰므로 game_id 단위 row를 선수 단위로 합산한다.
        if (playerIds.isEmpty())
            return Map.of();

        String sql = """
                select player_id, sum(point) as point
                from player_daily_point
                where date_id between :fromDateId and :toDateId
                  and player_id in (:playerIds)
                group by player_id
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("fromDateId", fromDateId)
                .addValue("toDateId", toDateId)
                .addValue("playerIds", playerIds);

        return namedParameterJdbcTemplate.query(sql, parameters, (rs, rowNum) ->
                Map.entry(rs.getLong("player_id"), rs.getInt("point"))
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private MapSqlParameterSource toParameterSource(PlayerDailyPoint dailyPoint) {
        return new MapSqlParameterSource()
                .addValue("dateId", dailyPoint.getDateId())
                .addValue("gameId", dailyPoint.getGameId())
                .addValue("playerId", dailyPoint.getPlayerId())
                .addValue("point", dailyPoint.getPoint())
                .addValue("createdAt", Timestamp.valueOf(dailyPoint.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(dailyPoint.getUpdatedAt()));
    }

}
