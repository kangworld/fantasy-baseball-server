package com.kangworld.fantasybaseball.player.image.dao.jdbc;

import com.kangworld.fantasybaseball.player.image.dao.PlayerImageDao;
import com.kangworld.fantasybaseball.player.image.model.PlayerImage;
import com.kangworld.fantasybaseball.player.image.model.PlayerImageRefreshTarget;
import com.kangworld.fantasybaseball.player.model.PlayerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PlayerImageDaoJdbc implements PlayerImageDao {

    private static final RowMapper<PlayerImage> ROW_MAPPER = new PlayerImageRowMapper();
    private static final RowMapper<PlayerImageRefreshTarget> REFRESH_TARGET_ROW_MAPPER = new PlayerImageRefreshTargetRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Map<Long, PlayerImage> findByPlayerIds(Set<Long> playerIds) {
        if (playerIds.isEmpty())
            return Map.of();

        String sql = """
                select *
                from player_image
                where player_id in (:playerIds)
                """;

        return namedParameterJdbcTemplate.query(
                        sql,
                        new MapSqlParameterSource("playerIds", playerIds),
                        ROW_MAPPER)
                .stream()
                .collect(Collectors.toMap(PlayerImage::getPlayerId, Function.identity()));
    }

    @Override
    public List<PlayerImageRefreshTarget> findRefreshTargets(LocalDateTime teamUpdatedFrom) {
        String sql = """
                select p.player_id, pi.img_path
                from player p
                left join player_image pi on pi.player_id = p.player_id
                where p.status = :status
                  and (
                      pi.player_id is null
                      or p.team_updated_at >= :teamUpdatedFrom
                  )
                order by p.player_id asc
                """;

        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("status", PlayerStatus.ACTIVE.name())
                        .addValue("teamUpdatedFrom", Timestamp.valueOf(teamUpdatedFrom)),
                REFRESH_TARGET_ROW_MAPPER);
    }

    @Override
    public void upsert(PlayerImage playerImage) {
        String sql = """
                insert into player_image values (
                    :playerId,
                    :imgPath,
                    :createdAt,
                    :updatedAt
                )
                on duplicate key update
                    img_path = values(img_path),
                    updated_at = values(updated_at)
                """;

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("playerId", playerImage.getPlayerId())
                .addValue("imgPath", playerImage.getImgPath())
                .addValue("createdAt", Timestamp.valueOf(playerImage.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(playerImage.getUpdatedAt())));
    }

    private static class PlayerImageRowMapper implements RowMapper<PlayerImage> {

        @Override
        public PlayerImage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return PlayerImage.builder()
                    .playerId(rs.getLong("player_id"))
                    .imgPath(rs.getString("img_path"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

    private static class PlayerImageRefreshTargetRowMapper implements RowMapper<PlayerImageRefreshTarget> {

        @Override
        public PlayerImageRefreshTarget mapRow(ResultSet rs, int rowNum) throws SQLException {
            return PlayerImageRefreshTarget.builder()
                    .playerId(rs.getLong("player_id"))
                    .imgPath(rs.getString("img_path"))
                    .build();
        }

    }

}
