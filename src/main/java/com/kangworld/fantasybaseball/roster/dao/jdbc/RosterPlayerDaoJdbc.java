package com.kangworld.fantasybaseball.roster.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RosterPlayerDaoJdbc implements RosterPlayerDao {

    private static final RowMapper<RosterPlayer> ROSTER_PLAYER_ROW_MAPPER =
            new RosterPlayerRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void insertAll(List<RosterPlayer> players) {
        if (players.isEmpty())
            return;

        String sql = """
                insert into roster_player values (
                    default,
                    :rosterId,
                    :rosterPosition,
                    :positionOrder,
                    :playerId,
                    :createdAt,
                    :updatedAt
                )
                """;

        MapSqlParameterSource[] batchParameters = players.stream()
                .map(this::toParameterSource)
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, batchParameters);
    }

    @Override
    public List<RosterPlayer> findByRosterId(long rosterId) {
        String sql = """
                select *
                from roster_player
                where roster_id = :rosterId
                order by roster_position asc, position_order asc
                """;

        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("rosterId", rosterId),
                ROSTER_PLAYER_ROW_MAPPER
        );
    }

    @Override
    public List<RosterPlayer> findByRosterIds(Set<Long> rosterIds) {
        if (rosterIds.isEmpty())
            return List.of();

        String sql = """
                select *
                from roster_player
                where roster_id in (:rosterIds)
                order by roster_id asc, roster_position asc, position_order asc
                """;

        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("rosterIds", rosterIds),
                ROSTER_PLAYER_ROW_MAPPER
        );
    }

    @Override
    public void deleteByRosterId(long rosterId) {
        String sql = """
                delete from roster_player
                where roster_id = :rosterId
                """;

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("rosterId", rosterId));
    }

    private MapSqlParameterSource toParameterSource(RosterPlayer player) {
        return new MapSqlParameterSource()
                .addValue("rosterId", player.getRosterId())
                .addValue("rosterPosition", player.getRosterPosition().name())
                .addValue("positionOrder", player.getPositionOrder())
                .addValue("playerId", player.getPlayerId())
                .addValue("createdAt", Timestamp.valueOf(player.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(player.getUpdatedAt()));
    }

    private static class RosterPlayerRowMapper implements RowMapper<RosterPlayer> {

        @Override
        public RosterPlayer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return RosterPlayer.builder()
                    .rosterPlayerId(rs.getLong("roster_player_id"))
                    .rosterId(rs.getLong("roster_id"))
                    .rosterPosition(PlayerPosition.valueOf(rs.getString("roster_position")))
                    .positionOrder(rs.getInt("position_order"))
                    .playerId(rs.getLong("player_id"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

}
