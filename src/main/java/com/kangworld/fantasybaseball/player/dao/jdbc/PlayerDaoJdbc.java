package com.kangworld.fantasybaseball.player.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.Player;
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
    public void upsertAll(List<Player> players) {
        if (players.isEmpty())
            return;

        // team_updated_at은 팀 코드가 바뀐 경우에만 갱신한다. 이미지 배치가 이 값을 기준으로 팀 변경 선수를 찾는다.
        String sql = """
                insert into player values (
                    :id,
                    :name,
                    :type,
                    :position,
                    :teamCode,
                    :status,
                    :teamUpdatedAt,
                    :createdAt,
                    :updatedAt
                )
                on duplicate key update
                    player_name = values(player_name),
                    player_type = values(player_type),
                    position = values(position),
                    team_updated_at = case
                        when team_code <> values(team_code) then values(team_updated_at)
                        else team_updated_at
                    end,
                    team_code = values(team_code),
                    status = values(status),
                    updated_at = values(updated_at)
                """;

        MapSqlParameterSource[] parameters = players.stream()
                .map(this::toParameterSource)
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, parameters);
    }

    @Override
    public List<PlayerSummary> findSummaries(PlayerSearchCondition condition) {
        StringBuilder sql = new StringBuilder("""
                select
                    p.player_id,
                    p.player_name,
                    p.player_type,
                    p.position,
                    p.team_code,
                    p.team_updated_at,
                    pp.cost,
                    pp.point,
                    pp.avg_point,
                    pi.img_path as img_path
                from player p
                join player_profile pp on pp.player_id = p.player_id
                left join player_image pi on pi.player_id = p.player_id
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
            sql.append(" and p.position = :positionCode");
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
                    p.position,
                    p.team_code,
                    p.team_updated_at,
                    pp.cost,
                    pp.point,
                    pp.avg_point,
                    pi.img_path as img_path
                from player p
                join player_profile pp on pp.player_id = p.player_id
                left join player_image pi on pi.player_id = p.player_id
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

    private MapSqlParameterSource toParameterSource(Player player) {
        return new MapSqlParameterSource()
                .addValue("id", player.getId())
                .addValue("name", player.getName())
                .addValue("type", player.getType().name())
                .addValue("position", player.getPosition().name())
                .addValue("teamCode", player.getTeamCode())
                .addValue("status", player.getStatus().name())
                .addValue("teamUpdatedAt", Timestamp.valueOf(player.getTeamUpdatedAt()))
                .addValue("createdAt", Timestamp.valueOf(player.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(player.getUpdatedAt()));
    }

    private static class PlayerSummaryRowMapper implements RowMapper<PlayerSummary> {

        @Override
        public PlayerSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            return PlayerSummary.builder()
                    .id(rs.getLong("player_id"))
                    .name(rs.getString("player_name"))
                    .type(PlayerType.valueOf(rs.getString("player_type")))
                    .position(PlayerPosition.valueOf(rs.getString("position")))
                    .cost(rs.getInt("cost"))
                    .point(rs.getInt("point"))
                    .avgPoint(rs.getInt("avg_point"))
                    .teamCode(rs.getString("team_code"))
                    .teamUpdatedAt(rs.getTimestamp("team_updated_at").toLocalDateTime())
                    .imgPath(rs.getString("img_path"))
                    .build();
        }

    }

}
