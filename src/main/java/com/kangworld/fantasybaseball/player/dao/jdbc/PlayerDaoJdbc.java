package com.kangworld.fantasybaseball.player.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerSearchCondition;
import com.kangworld.fantasybaseball.player.model.PlayerStatus;
import com.kangworld.fantasybaseball.player.model.PlayerSummary;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlayerDaoJdbc implements PlayerDao {

    private static final RowMapper<PlayerSummary> PLAYER_SUMMARY_ROW_MAPPER = new PlayerSummaryRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<PlayerSummary> findSummaries(PlayerSearchCondition condition) {
        StringBuilder sql = new StringBuilder("""
                select
                    p.player_id,
                    p.player_name,
                    p.player_type,
                    p.position_code,
                    pp.cost,
                    pp.point,
                    pp.avg_point,
                    pp.team_code,
                    pp.pos
                from player p
                join player_profile pp on pp.player_id = p.player_id
                where pp.date_id = :dateId
                  and p.status = :status
                """);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("dateId", condition.getDateId())
                .addValue("status", PlayerStatus.ACTIVE.name());

        if (condition.getPlayerType() != null) {
            sql.append(" and p.player_type = :playerType");
            parameters.addValue("playerType", condition.getPlayerType().name());
        }

        if (condition.getPositionCode() != null) {
            sql.append(" and p.position_code = :positionCode");
            parameters.addValue("positionCode", condition.getPositionCode().name());
        }

        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
            // 검색어는 프론트 입력 그대로 포함 검색한다. SQL wildcard는 파라미터로만 조립.
            sql.append(" and p.player_name like :keyword");
            parameters.addValue("keyword", "%" + condition.getKeyword() + "%");
        }

        sql.append(" order by pp.point desc, p.player_name asc");

        return namedParameterJdbcTemplate.query(sql.toString(), parameters, PLAYER_SUMMARY_ROW_MAPPER);
    }

    @Override
    public List<PlayerSummary> findSummariesByPlayerIds(int dateId, Set<Long> playerIds) {
        if (playerIds.isEmpty())
            return List.of();

        String sql = """
                select
                    p.player_id,
                    p.player_name,
                    p.player_type,
                    p.position_code,
                    pp.cost,
                    pp.point,
                    pp.avg_point,
                    pp.team_code,
                    pp.pos
                from player p
                join player_profile pp on pp.player_id = p.player_id
                where pp.date_id = :dateId
                  and p.status = :status
                  and p.player_id in (:playerIds)
                order by p.player_id asc
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("dateId", dateId)
                .addValue("status", PlayerStatus.ACTIVE.name())
                .addValue("playerIds", playerIds);

        return namedParameterJdbcTemplate.query(sql, parameters, PLAYER_SUMMARY_ROW_MAPPER);
    }

    private static class PlayerSummaryRowMapper implements RowMapper<PlayerSummary> {

        @Override
        public PlayerSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return PlayerSummary.builder()
                    .playerId(rs.getLong("player_id"))
                    .playerName(rs.getString("player_name"))
                    .playerType(PlayerType.valueOf(rs.getString("player_type")))
                    .positionCode(PlayerPosition.valueOf(rs.getString("position_code")))
                    .cost(rs.getInt("cost"))
                    .point(rs.getInt("point"))
                    .avgPoint(rs.getInt("avg_point"))
                    .teamCode(rs.getString("team_code"))
                    .pos(rs.getString("pos"))
                    .build();
        }

    }

}
