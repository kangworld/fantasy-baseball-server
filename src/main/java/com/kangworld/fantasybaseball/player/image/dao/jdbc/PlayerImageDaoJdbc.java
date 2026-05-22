package com.kangworld.fantasybaseball.player.image.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.player.image.dao.PlayerImageDao;
import com.kangworld.fantasybaseball.player.image.model.PlayerImage;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlayerImageDaoJdbc implements PlayerImageDao {

    private static final RowMapper<PlayerImage> ROW_MAPPER = new PlayerImageRowMapper();

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

}
