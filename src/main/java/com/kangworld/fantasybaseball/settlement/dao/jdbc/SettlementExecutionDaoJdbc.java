package com.kangworld.fantasybaseball.settlement.dao.jdbc;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
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

import com.kangworld.fantasybaseball.settlement.dao.SettlementExecutionDao;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecution;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecutionStatus;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementExecutionDaoJdbc implements SettlementExecutionDao {

    private static final RowMapper<SettlementExecution> ROW_MAPPER = new SettlementExecutionRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public long insertStarted(LocalDate targetDate, LocalDateTime now) {
        String sql = """
                insert into settlement_execution (
                    target_date,
                    status,
                    started_at,
                    finished_at,
                    failure_step,
                    failure_message,
                    created_at,
                    updated_at
                ) values (
                    :targetDate,
                    :status,
                    :startedAt,
                    null,
                    null,
                    null,
                    :createdAt,
                    :updatedAt
                )
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("targetDate", Date.valueOf(targetDate))
                .addValue("status", SettlementExecutionStatus.STARTED.name())
                .addValue("startedAt", Timestamp.valueOf(now))
                .addValue("createdAt", Timestamp.valueOf(now))
                .addValue("updatedAt", Timestamp.valueOf(now));
        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, parameters, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void updateDone(long settlementExecutionId, LocalDateTime now) {
        String sql = """
                update settlement_execution
                set status = :status,
                    finished_at = :finishedAt,
                    updated_at = :updatedAt
                where settlement_execution_id = :settlementExecutionId
                """;

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("settlementExecutionId", settlementExecutionId)
                .addValue("status", SettlementExecutionStatus.DONE.name())
                .addValue("finishedAt", Timestamp.valueOf(now))
                .addValue("updatedAt", Timestamp.valueOf(now)));
    }

    @Override
    public void updateFailed(
            long settlementExecutionId,
            SettlementStepName failureStep,
            String failureMessage,
            LocalDateTime now) {
        String sql = """
                update settlement_execution
                set status = :status,
                    finished_at = :finishedAt,
                    failure_step = :failureStep,
                    failure_message = :failureMessage,
                    updated_at = :updatedAt
                where settlement_execution_id = :settlementExecutionId
                """;

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("settlementExecutionId", settlementExecutionId)
                .addValue("status", SettlementExecutionStatus.FAILED.name())
                .addValue("finishedAt", Timestamp.valueOf(now))
                .addValue("failureStep", failureStep.name())
                .addValue("failureMessage", failureMessage)
                .addValue("updatedAt", Timestamp.valueOf(now)));
    }

    @Override
    public Optional<SettlementExecution> findLatestByTargetDate(LocalDate targetDate) {
        String sql = """
                select *
                from settlement_execution
                where target_date = :targetDate
                order by settlement_execution_id desc
                limit 1
                """;

        try {
            SettlementExecution execution = namedParameterJdbcTemplate.queryForObject(
                    sql,
                    Map.of("targetDate", Date.valueOf(targetDate)),
                    ROW_MAPPER
            );

            return Optional.ofNullable(execution);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByTargetDateAndStatus(LocalDate targetDate, SettlementExecutionStatus status) {
        String sql = """
                select count(*)
                from settlement_execution
                where target_date = :targetDate
                  and status = :status
                """;

        Integer count = namedParameterJdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource()
                        .addValue("targetDate", Date.valueOf(targetDate))
                        .addValue("status", status.name()),
                Integer.class
        );

        return count != null && count > 0;
    }

    private static class SettlementExecutionRowMapper implements RowMapper<SettlementExecution> {

        @Override
        public SettlementExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
            return SettlementExecution.builder()
                    .settlementExecutionId(rs.getLong("settlement_execution_id"))
                    .targetDate(rs.getDate("target_date").toLocalDate())
                    .status(SettlementExecutionStatus.valueOf(rs.getString("status")))
                    .startedAt(toLocalDateTime(rs.getTimestamp("started_at")))
                    .finishedAt(toLocalDateTime(rs.getTimestamp("finished_at")))
                    .failureStep(toFailureStep(rs.getString("failure_step")))
                    .failureMessage(rs.getString("failure_message"))
                    .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                    .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                    .build();
        }

        private SettlementStepName toFailureStep(String failureStep) {
            if (failureStep == null)
                return null;

            return SettlementStepName.valueOf(failureStep);
        }

        private LocalDateTime toLocalDateTime(Timestamp timestamp) {
            if (timestamp == null)
                return null;

            return timestamp.toLocalDateTime();
        }

    }

}
