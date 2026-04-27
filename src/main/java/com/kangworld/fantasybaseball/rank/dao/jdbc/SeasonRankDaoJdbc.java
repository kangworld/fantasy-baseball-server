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

import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.rank.model.SeasonRank;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SeasonRankDaoJdbc implements SeasonRankDao {

    private static final RowMapper<SeasonRank> ROW_MAPPER = new SeasonRankRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void insertAll(List<SeasonRank> ranks) {
        if (ranks.isEmpty())
            return;

        String sql = """
                insert into season_rank (
                    date_id,
                    rank_no,
                    member_id,
                    point,
                    daily_point,
                    display_rank_no,
                    previous_rank_no,
                    created_at,
                    updated_at
                ) values (
                    :dateId,
                    :rankNo,
                    :memberId,
                    :point,
                    :dailyPoint,
                    :displayRankNo,
                    :previousRankNo,
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
    public List<SeasonRank> findByDateId(int dateId) {
        String sql = """
                select
                    date_id,
                    rank_no,
                    member_id,
                    point,
                    daily_point,
                    display_rank_no,
                    previous_rank_no,
                    created_at,
                    updated_at
                from season_rank
                where date_id = :dateId
                order by rank_no asc
                """;

        return namedParameterJdbcTemplate.query(sql, Map.of("dateId", dateId), ROW_MAPPER);
    }

    @Override
    public Optional<SeasonRank> findByMemberIdAndDateId(long memberId, int dateId) {
        String sql = """
                select
                    date_id,
                    rank_no,
                    member_id,
                    point,
                    daily_point,
                    display_rank_no,
                    previous_rank_no,
                    created_at,
                    updated_at
                from season_rank
                where member_id = :memberId
                  and date_id = :dateId
                """;

        try {
            SeasonRank rank = namedParameterJdbcTemplate.queryForObject(
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
    public void deleteByDateId(int dateId) {
        String sql = "delete from season_rank where date_id = :dateId";

        namedParameterJdbcTemplate.update(sql, Map.of("dateId", dateId));
    }

    private MapSqlParameterSource toParameterSource(SeasonRank rank) {
        return new MapSqlParameterSource()
                .addValue("dateId", rank.getDateId())
                .addValue("rankNo", rank.getRankNo())
                .addValue("memberId", rank.getMemberId())
                .addValue("point", rank.getPoint())
                .addValue("dailyPoint", rank.getDailyPoint())
                .addValue("displayRankNo", rank.getDisplayRankNo())
                .addValue("previousRankNo", rank.getPreviousRankNo())
                .addValue("createdAt", Timestamp.valueOf(rank.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(rank.getUpdatedAt()));
    }

    private static class SeasonRankRowMapper implements RowMapper<SeasonRank> {

        @Override
        public SeasonRank mapRow(ResultSet rs, int rowNum) throws SQLException {

            return SeasonRank.builder()
                    .dateId(rs.getInt("date_id"))
                    .rankNo(rs.getInt("rank_no"))
                    .memberId(rs.getLong("member_id"))
                    .point(rs.getInt("point"))
                    .dailyPoint(rs.getInt("daily_point"))
                    .displayRankNo(rs.getInt("display_rank_no"))
                    .previousRankNo(rs.getInt("previous_rank_no"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

}
