package com.kangworld.fantasybaseball.settlement.dao;

import java.time.LocalDateTime;

import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;

public interface SettlementStepExecutionDao {

    long insertStarted(long settlementExecutionId, SettlementStepName stepName, LocalDateTime now);

    void failStartedSteps(long settlementExecutionId, String message, LocalDateTime now);

    void updateDone(long settlementStepExecutionId, LocalDateTime now);

    void updateFailed(
            long settlementStepExecutionId,
            String failureType,
            String failureMessage,
            LocalDateTime now);

}
