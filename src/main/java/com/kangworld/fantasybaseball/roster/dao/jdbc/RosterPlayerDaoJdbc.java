package com.kangworld.fantasybaseball.roster.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;
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
                    :slotNo,
                    :playerId,
                    :playerType,
                    :positionCode,
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
                order by slot_no asc
                """;

        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("rosterId", rosterId),
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
                .addValue("slotNo", player.getSlotNo())
                .addValue("playerId", player.getPlayerId())
                .addValue("playerType", player.getPlayerType().name())
                .addValue("positionCode", player.getPositionCode().name())
                .addValue("createdAt", Timestamp.valueOf(player.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(player.getUpdatedAt()));
    }

    private static class RosterPlayerRowMapper implements RowMapper<RosterPlayer> {

        @Override
        public RosterPlayer mapRow(ResultSet rs, int rowNum) throws SQLException {

            return RosterPlayer.builder()
                    .rosterPlayerId(rs.getLong("roster_player_id"))
                    .rosterId(rs.getLong("roster_id"))
                    .slotNo(rs.getInt("slot_no"))
                    .playerId(rs.getLong("player_id"))
                    .playerType(PlayerType.valueOf(rs.getString("player_type")))
                    .positionCode(PlayerPosition.valueOf(rs.getString("position_code")))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

}
