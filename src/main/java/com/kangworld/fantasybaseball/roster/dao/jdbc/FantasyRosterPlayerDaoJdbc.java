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
import com.kangworld.fantasybaseball.roster.dao.FantasyRosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.FantasyRosterPlayer;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FantasyRosterPlayerDaoJdbc implements FantasyRosterPlayerDao {

    private static final RowMapper<FantasyRosterPlayer> FANTASY_ROSTER_PLAYER_ROW_MAPPER =
            new FantasyRosterPlayerRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void insertAll(List<FantasyRosterPlayer> players) {
        if (players.isEmpty()) {
            return;
        }

        String sql = """
                insert into fantasy_roster_player (
                    roster_id,
                    slot_no,
                    player_id,
                    player_type,
                    position_code,
                    created_at,
                    updated_at
                ) values (
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
    public List<FantasyRosterPlayer> findByRosterId(long rosterId) {
        String sql = """
                select
                    roster_player_id,
                    roster_id,
                    slot_no,
                    player_id,
                    player_type,
                    position_code,
                    created_at,
                    updated_at
                from fantasy_roster_player
                where roster_id = :rosterId
                order by slot_no asc
                """;

        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("rosterId", rosterId),
                FANTASY_ROSTER_PLAYER_ROW_MAPPER
        );
    }

    private MapSqlParameterSource toParameterSource(FantasyRosterPlayer player) {
        return new MapSqlParameterSource()
                .addValue("rosterId", player.getRosterId())
                .addValue("slotNo", player.getSlotNo())
                .addValue("playerId", player.getPlayerId())
                .addValue("playerType", player.getPlayerType().name())
                .addValue("positionCode", player.getPositionCode().name())
                .addValue("createdAt", Timestamp.valueOf(player.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(player.getUpdatedAt()));
    }

    private static class FantasyRosterPlayerRowMapper implements RowMapper<FantasyRosterPlayer> {

        @Override
        public FantasyRosterPlayer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return FantasyRosterPlayer.builder()
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
