package com.kangworld.fantasybaseball.roster.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.roster.dao.FantasyRosterDao;
import com.kangworld.fantasybaseball.roster.model.FantasyRoster;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FantasyRosterDaoJdbc implements FantasyRosterDao {

    private static final RowMapper<FantasyRoster> FANTASY_ROSTER_ROW_MAPPER = new FantasyRosterRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public long insert(FantasyRoster roster) {
        String sql = """
                insert into fantasy_roster (
                    member_id,
                    date_id,
                    created_at,
                    updated_at
                ) values (
                    :memberId,
                    :dateId,
                    :createdAt,
                    :updatedAt
                )
                """;

        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("memberId", roster.getMemberId())
                .addValue("dateId", roster.getDateId())
                .addValue("createdAt", Timestamp.valueOf(roster.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(roster.getUpdatedAt()));

        // 로스터 선수 목록 저장에서 roster_id가 바로 필요해서 생성 키를 회수한다.
        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, parameterSource, keyHolder, new String[]{"roster_id"});

        Number key = keyHolder.getKey();
        if (key == null)
            throw new IllegalStateException("fantasy roster insert key is null");

        return key.longValue();
    }

    @Override
    public Optional<FantasyRoster> findByMemberIdAndDateId(long memberId, int dateId) {
        String sql = """
                select
                    roster_id,
                    member_id,
                    date_id,
                    created_at,
                    updated_at
                from fantasy_roster
                where member_id = :memberId
                  and date_id = :dateId
                """;

        try {
            FantasyRoster roster = namedParameterJdbcTemplate.queryForObject(
                    sql,
                    Map.of(
                            "memberId", memberId,
                            "dateId", dateId
                    ),
                    FANTASY_ROSTER_ROW_MAPPER
            );

            return Optional.ofNullable(roster);
        } catch (EmptyResultDataAccessException exception) {

            return Optional.empty();
        }
    }

    @Override
    public void updateTimestamp(long rosterId, LocalDateTime updatedAt) {
        String sql = """
                update fantasy_roster
                set updated_at = :updatedAt
                where roster_id = :rosterId
                """;

        namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("rosterId", rosterId)
                        .addValue("updatedAt", Timestamp.valueOf(updatedAt))
        );
    }

    private static class FantasyRosterRowMapper implements RowMapper<FantasyRoster> {

        @Override
        public FantasyRoster mapRow(ResultSet rs, int rowNum) throws SQLException {

            return FantasyRoster.builder()
                    .rosterId(rs.getLong("roster_id"))
                    .memberId(rs.getLong("member_id"))
                    .dateId(rs.getInt("date_id"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

}
