package com.kangworld.fantasybaseball.rank.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.dao.view.DailyRankView;
import com.kangworld.fantasybaseball.rank.model.DailyRank;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DailyRankDaoJdbc implements DailyRankDao {

    private static final RowMapper<DailyRank> ROW_MAPPER = new DailyRankRowMapper();
    private static final RowMapper<DailyRankView> VIEW_ROW_MAPPER = new DailyRankViewRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void insertAll(List<DailyRank> ranks) {
        if (ranks.isEmpty())
            return;

        String sql = """
                insert into daily_rank values (
                    :dateId,
                    :rankNo,
                    :memberId,
                    :point,
                    :displayRankNo,
                    :createdAt,
                    :updatedAt
                )
                """;

        MapSqlParameterSource[] parameters = ranks.stream()
                .map(this::toParameterSource)
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, parameters);
    }

    @Override
    public List<DailyRank> findByDateId(int dateId) {
        String sql = """
                select *
                from daily_rank
                where date_id = :dateId
                order by rank_no asc
                """;

        return namedParameterJdbcTemplate.query(sql, Map.of("dateId", dateId), ROW_MAPPER);
    }

    @Override
    public Optional<DailyRank> findByMemberIdAndDateId(long memberId, int dateId) {
        String sql = """
                select *
                from daily_rank
                where member_id = :memberId
                  and date_id = :dateId
                """;

        try {
            DailyRank rank = namedParameterJdbcTemplate.queryForObject(
                    sql,
                    Map.of("memberId", memberId, "dateId", dateId),
                    ROW_MAPPER
            );

            return Optional.ofNullable(rank);
        } catch (EmptyResultDataAccessException exception) {

            return Optional.empty();
        }
    }

    @Override
    public List<DailyRankView> findViewsByDateId(int dateId, int limit, int offset) {
        String sql = """
                select dr.rank_no, dr.display_rank_no, dr.member_id, m.display_name, dr.point
                from daily_rank dr
                join member m on m.member_id = dr.member_id
                where dr.date_id = :dateId
                order by dr.rank_no asc
                limit :limit offset :offset
                """;

        return namedParameterJdbcTemplate.query(
                sql,
                Map.of("dateId", dateId, "limit", limit, "offset", offset),
                VIEW_ROW_MAPPER
        );
    }

    @Override
    public Optional<DailyRankView> findViewByMemberIdAndDateId(long memberId, int dateId) {
        String sql = """
                select dr.rank_no, dr.display_rank_no, dr.member_id, m.display_name, dr.point
                from daily_rank dr
                join member m on m.member_id = dr.member_id
                where dr.member_id = :memberId
                  and dr.date_id = :dateId
                """;

        try {
            DailyRankView rank = namedParameterJdbcTemplate.queryForObject(
                    sql,
                    Map.of("memberId", memberId, "dateId", dateId),
                    VIEW_ROW_MAPPER
            );

            return Optional.ofNullable(rank);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteByDateId(int dateId) {
        String sql = "delete from daily_rank where date_id = :dateId";

        namedParameterJdbcTemplate.update(sql, Map.of("dateId", dateId));
    }

    private MapSqlParameterSource toParameterSource(DailyRank rank) {
        return new MapSqlParameterSource()
                .addValue("dateId", rank.getDateId())
                .addValue("rankNo", rank.getRankNo())
                .addValue("memberId", rank.getMemberId())
                .addValue("point", rank.getPoint())
                .addValue("displayRankNo", rank.getDisplayRankNo())
                .addValue("createdAt", Timestamp.valueOf(rank.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(rank.getUpdatedAt()));
    }

    private static class DailyRankRowMapper implements RowMapper<DailyRank> {

        @Override
        public DailyRank mapRow(ResultSet rs, int rowNum) throws SQLException {

            return DailyRank.builder()
                    .dateId(rs.getInt("date_id"))
                    .rankNo(rs.getInt("rank_no"))
                    .memberId(rs.getLong("member_id"))
                    .point(rs.getInt("point"))
                    .displayRankNo(rs.getInt("display_rank_no"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

    private static class DailyRankViewRowMapper implements RowMapper<DailyRankView> {

        @Override
        public DailyRankView mapRow(ResultSet rs, int rowNum) throws SQLException {
            return DailyRankView.builder()
                    .rankNo(rs.getInt("rank_no"))
                    .displayRankNo(rs.getInt("display_rank_no"))
                    .memberId(rs.getLong("member_id"))
                    .displayName(rs.getString("display_name"))
                    .point(rs.getInt("point"))
                    .build();
        }

    }

}
