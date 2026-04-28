package com.kangworld.fantasybaseball.point.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.point.dao.PointRuleDao;
import com.kangworld.fantasybaseball.point.model.BatterPointRecordCode;
import com.kangworld.fantasybaseball.point.model.PointRule;
import com.kangworld.fantasybaseball.point.model.PitcherPointRecordCode;
import com.kangworld.fantasybaseball.point.model.PointRecordCode;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PointRuleDaoJdbc implements PointRuleDao {

    private static final RowMapper<PointRule> ROW_MAPPER = new PointRuleRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<PointRule> findAllActive() {
        String sql = """
                select *
                from point_rule
                where active = true
                order by record_code asc
                """;

        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource(),
                ROW_MAPPER
        );
    }

    private static class PointRuleRowMapper implements RowMapper<PointRule> {

        @Override
        public PointRule mapRow(ResultSet rs, int rowNum) throws SQLException {
            PlayerType playerType = PlayerType.valueOf(rs.getString("player_type"));

            return PointRule.builder()
                    .playerType(playerType)
                    .recordCode(toRecordCode(playerType, rs.getString("record_code")))
                    .point(rs.getInt("point"))
                    .description(rs.getString("description"))
                    .active(rs.getBoolean("active"))
                    .build();
        }

        private PointRecordCode toRecordCode(PlayerType playerType, String recordCode) {
            return switch (playerType) {
                case BATTER -> BatterPointRecordCode.valueOf(recordCode);
                case PITCHER -> PitcherPointRecordCode.valueOf(recordCode);
            };
        }

    }

}
