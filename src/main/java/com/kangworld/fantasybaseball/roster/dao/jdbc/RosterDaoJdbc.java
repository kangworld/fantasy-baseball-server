package com.kangworld.fantasybaseball.roster.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.model.Roster;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RosterDaoJdbc implements RosterDao {

    private static final RowMapper<Roster> ROSTER_ROW_MAPPER = new RosterRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public long insert(Roster roster) {
        String sql = """
                insert into roster values (
                    default,
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
            throw new IllegalStateException("roster insert key is null");

        return key.longValue();
    }

    @Override
    public Optional<Roster> findByMemberIdAndDateId(long memberId, int dateId) {
        String sql = """
                select *
                from roster
                where member_id = :memberId
                  and date_id = :dateId
                """;

        try {
            Roster roster = namedParameterJdbcTemplate.queryForObject(
                    sql,
                    Map.of(
                            "memberId", memberId,
                            "dateId", dateId
                    ),
                    ROSTER_ROW_MAPPER
            );

            return Optional.ofNullable(roster);
        } catch (EmptyResultDataAccessException exception) {

            return Optional.empty();
        }
    }

    @Override
    public List<Roster> findByDateId(int dateId) {
        String sql = """
                select *
                from roster
                where date_id = :dateId
                order by member_id asc
                """;

        return namedParameterJdbcTemplate.query(sql, Map.of("dateId", dateId), ROSTER_ROW_MAPPER);
    }

    @Override
    public void updateTimestamp(long rosterId, LocalDateTime updatedAt) {
        String sql = """
                update roster
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

    private static class RosterRowMapper implements RowMapper<Roster> {

        @Override
        public Roster mapRow(ResultSet rs, int rowNum) throws SQLException {

            return Roster.builder()
                    .rosterId(rs.getLong("roster_id"))
                    .memberId(rs.getLong("member_id"))
                    .dateId(rs.getInt("date_id"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

}
