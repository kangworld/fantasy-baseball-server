package com.kangworld.fantasybaseball.game.dao.jdbc;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.game.dao.GameDao;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GameDaoJdbc implements GameDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Set<String> findStartedTeamCodes(LocalDate date, LocalDateTime now, Set<String> teamCodes) {
        if (teamCodes.isEmpty())
            return Set.of();

        String sql = """
                select home_team_code, away_team_code
                from game
                where date(start_at) = :date
                  and start_at <= :now
                  and (
                      home_team_code in (:teamCodes)
                      or away_team_code in (:teamCodes)
                  )
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("date", date)
                .addValue("now", Timestamp.valueOf(now))
                .addValue("teamCodes", teamCodes);

        List<String> startedTeamCodes = namedParameterJdbcTemplate.query(sql, parameters, (rs, rowNum) -> {
            Set<String> codes = new HashSet<>();
            codes.add(rs.getString("home_team_code"));
            codes.add(rs.getString("away_team_code"));

            return codes;
        }).stream()
                .flatMap(Set::stream)
                .filter(teamCodes::contains)
                .toList();

        return new HashSet<>(startedTeamCodes);
    }

}
