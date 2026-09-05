package com.kangworld.fantasybaseball.point.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlayerProfilePointDaoJdbc implements PlayerProfilePointDao {

    private static final RowMapper<PlayerProfile> ROW_MAPPER = new PlayerProfileRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void deleteByDateId(int dateId) {
        namedParameterJdbcTemplate.update(
                "delete from player_profile where date_id = :dateId",
                new MapSqlParameterSource("dateId", dateId)
        );
    }

    @Override
    public List<PlayerProfile> findByDateId(int dateId) {
        String sql = """
                select *
                from player_profile
                where date_id = :dateId
                order by player_id asc
                """;

        return namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource("dateId", dateId), ROW_MAPPER);
    }

    @Override
    public List<PlayerProfile> findByDateIds(Set<Integer> dateIds) {
        if (dateIds.isEmpty())
            return List.of();

        String sql = """
                select *
                from player_profile
                where date_id in (:dateIds)
                order by date_id asc, player_id asc
                """;

        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("dateIds", dateIds),
                ROW_MAPPER);
    }

    @Override
    public void insertAll(List<PlayerProfile> profiles) {
        if (profiles.isEmpty())
            return;

        String sql = """
                insert into player_profile values (
                    :dateId,
                    :playerId,
                    :cost,
                    :point,
                    :avgPoint,
                    :pointGameCount,
                    :createdAt,
                    :updatedAt
                )
                """;

        MapSqlParameterSource[] parameters = profiles.stream()
                .map(this::toParameterSource)
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, parameters);
    }

    @Override
    public void updateCostAll(List<PlayerProfile> profiles) {
        if (profiles.isEmpty())
            return;

        String sql = """
                update player_profile
                set cost = :cost,
                    updated_at = :updatedAt
                where date_id = :dateId
                  and player_id = :playerId
                """;

        MapSqlParameterSource[] parameters = profiles.stream()
                .map(this::toParameterSource)
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, parameters);
    }

    private MapSqlParameterSource toParameterSource(PlayerProfile profile) {
        return new MapSqlParameterSource()
                .addValue("dateId", profile.getDateId())
                .addValue("playerId", profile.getPlayerId())
                .addValue("cost", profile.getCost())
                .addValue("point", profile.getPoint())
                .addValue("avgPoint", profile.getAvgPoint())
                .addValue("pointGameCount", profile.getPointGameCount())
                .addValue("createdAt", Timestamp.valueOf(profile.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(profile.getUpdatedAt()));
    }

    private static class PlayerProfileRowMapper implements RowMapper<PlayerProfile> {

        @Override
        public PlayerProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return PlayerProfile.builder()
                    .dateId(rs.getInt("date_id"))
                    .playerId(rs.getLong("player_id"))
                    .cost(rs.getInt("cost"))
                    .point(rs.getInt("point"))
                    .avgPoint(rs.getInt("avg_point"))
                    .pointGameCount(rs.getInt("point_game_count"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

}
