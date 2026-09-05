package com.kangworld.fantasybaseball.settlement.dao.jdbc;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.settlement.dao.SettlementStepExecutionDao;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementStepExecutionDaoJdbc implements SettlementStepExecutionDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public long insertStarted(long settlementExecutionId, SettlementStepName stepName, LocalDateTime now) {
        String sql = """
                insert into settlement_step_execution (
                    settlement_execution_id,
                    step_name,
                    status,
                    started_at,
                    finished_at,
                    failure_type,
                    failure_message,
                    created_at,
                    updated_at
                ) values (
                    :settlementExecutionId,
                    :stepName,
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
                .addValue("settlementExecutionId", settlementExecutionId)
                .addValue("stepName", stepName.name())
                .addValue("status", SettlementStepStatus.STARTED.name())
                .addValue("startedAt", Timestamp.valueOf(now))
                .addValue("createdAt", Timestamp.valueOf(now))
                .addValue("updatedAt", Timestamp.valueOf(now));
        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, parameters, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void failStartedSteps(long settlementExecutionId, String message, LocalDateTime now) {
        String sql = """
                update settlement_step_execution
                set status = 'FAILED',
                    finished_at = :now,
                    failure_type = 'INTERRUPTED',
                    failure_message = :message,
                    updated_at = :now
                where settlement_execution_id = :id and status = 'STARTED'
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", settlementExecutionId)
                .addValue("message", message)
                .addValue("now", Timestamp.valueOf(now)));
    }

    @Override
    public void updateDone(long settlementStepExecutionId, LocalDateTime now) {
        String sql = """
                update settlement_step_execution
                set status = :status,
                    finished_at = :finishedAt,
                    updated_at = :updatedAt
                where settlement_step_execution_id = :settlementStepExecutionId
                """;

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("settlementStepExecutionId", settlementStepExecutionId)
                .addValue("status", SettlementStepStatus.DONE.name())
                .addValue("finishedAt", Timestamp.valueOf(now))
                .addValue("updatedAt", Timestamp.valueOf(now)));
    }

    @Override
    public void updateFailed(
            long settlementStepExecutionId,
            String failureType,
            String failureMessage,
            LocalDateTime now) {
        String sql = """
                update settlement_step_execution
                set status = :status,
                    finished_at = :finishedAt,
                    failure_type = :failureType,
                    failure_message = :failureMessage,
                    updated_at = :updatedAt
                where settlement_step_execution_id = :settlementStepExecutionId
                """;

        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("settlementStepExecutionId", settlementStepExecutionId)
                .addValue("status", SettlementStepStatus.FAILED.name())
                .addValue("finishedAt", Timestamp.valueOf(now))
                .addValue("failureType", failureType)
                .addValue("failureMessage", failureMessage)
                .addValue("updatedAt", Timestamp.valueOf(now)));
    }

}
