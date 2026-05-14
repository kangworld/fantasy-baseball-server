package com.kangworld.fantasybaseball.settlement.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.kangworld.fantasybaseball.settlement.model.SettlementExecution;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecutionStatus;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;

public interface SettlementExecutionDao {

    long insertStarted(LocalDate targetDate, LocalDateTime now);

    void updateDone(long settlementExecutionId, LocalDateTime now);

    void updateFailed(
            long settlementExecutionId,
            SettlementStepName failureStep,
            String failureMessage,
            LocalDateTime now);

    Optional<SettlementExecution> findLatestByTargetDate(LocalDate targetDate);

    boolean existsByTargetDateAndStatus(LocalDate targetDate, SettlementExecutionStatus status);

}
