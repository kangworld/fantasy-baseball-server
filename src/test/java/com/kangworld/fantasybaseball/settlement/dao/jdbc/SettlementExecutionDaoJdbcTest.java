package com.kangworld.fantasybaseball.settlement.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.settlement.dao.SettlementExecutionDao;
import com.kangworld.fantasybaseball.settlement.dao.SettlementStepExecutionDao;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecution;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecutionStatus;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "delete from settlement_step_execution",
        "delete from settlement_execution"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SettlementExecutionDaoJdbcTest {

    @Autowired
    private SettlementExecutionDao settlementExecutionDao;

    @Autowired
    private SettlementStepExecutionDao settlementStepExecutionDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertStarted는_정산_실행을_STARTED로_저장한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 11);
        LocalDateTime now = LocalDateTime.of(2026, 5, 12, 2, 0);

        // when
        long executionId = settlementExecutionDao.insertStarted(targetDate, now);

        // then
        Optional<SettlementExecution> execution = settlementExecutionDao.findLatestByTargetDate(targetDate);
        assertThat(execution).isPresent();
        assertThat(execution.get().getSettlementExecutionId()).isEqualTo(executionId);
        assertThat(execution.get().getStatus()).isEqualTo(SettlementExecutionStatus.STARTED);
    }

    @Test
    void updateFailed는_failure_컬럼에_실패_정보를_저장한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 11);
        LocalDateTime now = LocalDateTime.of(2026, 5, 12, 2, 0);
        long executionId = settlementExecutionDao.insertStarted(targetDate, now);

        // when
        settlementExecutionDao.updateFailed(
                executionId,
                SettlementStepName.DAILY_RANK,
                "daily rank failed",
                now.plusMinutes(1)
        );

        // then
        SettlementExecution execution = settlementExecutionDao.findLatestByTargetDate(targetDate).get();
        assertThat(execution.getStatus()).isEqualTo(SettlementExecutionStatus.FAILED);
        assertThat(execution.getFailureStep()).isEqualTo(SettlementStepName.DAILY_RANK);
        assertThat(execution.getFailureMessage()).isEqualTo("daily rank failed");
    }

    @Test
    void stepExecution은_단계_실패_타입과_메시지를_저장한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 11);
        LocalDateTime now = LocalDateTime.of(2026, 5, 12, 2, 0);
        long executionId = settlementExecutionDao.insertStarted(targetDate, now);
        long stepExecutionId = settlementStepExecutionDao.insertStarted(executionId, SettlementStepName.PLAYER_POINT, now);

        // when
        settlementStepExecutionDao.updateFailed(
                stepExecutionId,
                IllegalStateException.class.getName(),
                "player point failed",
                now.plusMinutes(1)
        );

        // then
        String failureType = jdbcTemplate.queryForObject(
                "select failure_type from settlement_step_execution where settlement_step_execution_id = ?",
                String.class,
                stepExecutionId
        );
        String failureMessage = jdbcTemplate.queryForObject(
                "select failure_message from settlement_step_execution where settlement_step_execution_id = ?",
                String.class,
                stepExecutionId
        );
        assertThat(failureType).isEqualTo(IllegalStateException.class.getName());
        assertThat(failureMessage).isEqualTo("player point failed");
    }

    @Test
    void existsByTargetDateAndStatus는_DONE_정산_존재_여부를_반환한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 11);
        LocalDateTime now = LocalDateTime.of(2026, 5, 12, 2, 0);
        long executionId = settlementExecutionDao.insertStarted(targetDate, now);
        settlementExecutionDao.updateDone(executionId, now.plusMinutes(1));

        // when then
        assertThat(settlementExecutionDao.existsByTargetDateAndStatus(targetDate, SettlementExecutionStatus.DONE))
                .isTrue();
        assertThat(settlementExecutionDao.existsByTargetDateAndStatus(targetDate, SettlementExecutionStatus.FAILED))
                .isFalse();
    }

}
