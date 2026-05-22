package com.kangworld.fantasybaseball.settlement.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.settlement.dao.SettlementExecutionDao;
import com.kangworld.fantasybaseball.settlement.dao.SettlementStepExecutionDao;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecutionStatus;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementExecutionRecorder {

    private final SettlementExecutionDao settlementExecutionDao;
    private final SettlementStepExecutionDao settlementStepExecutionDao;
    private final FantasyTime fantasyTime;

    public boolean canStart(LocalDate targetDate) {
        // 전날 정산이 실패하거나 진행 중이면 당일 정산을 시작하지 않는다.
        return settlementExecutionDao.findLatestByTargetDate(targetDate.minusDays(1))
                .map(execution -> execution.getStatus() == SettlementExecutionStatus.DONE)
                .orElse(true);
    }

    public long start(LocalDate targetDate) {
        return settlementExecutionDao.insertStarted(targetDate, fantasyTime.getDateTime());
    }

    public void runStep(long settlementExecutionId, SettlementStepName stepName, Runnable task) {
        runStepWithResult(settlementExecutionId, stepName, () -> {
            task.run();

            return null;
        });
    }

    public <T> T runStepWithResult(long settlementExecutionId, SettlementStepName stepName, Supplier<T> task) {
        long stepExecutionId = settlementStepExecutionDao.insertStarted(
                settlementExecutionId,
                stepName,
                fantasyTime.getDateTime()
        );

        try {
            T result = task.get();
            settlementStepExecutionDao.updateDone(stepExecutionId, fantasyTime.getDateTime());

            return result;
        } catch (RuntimeException exception) {
            LocalDateTime now = fantasyTime.getDateTime();
            settlementStepExecutionDao.updateFailed(
                    stepExecutionId,
                    exception.getClass().getName(),
                    exception.getMessage(),
                    now
            );
            settlementExecutionDao.updateFailed(settlementExecutionId, stepName, exception.getMessage(), now);
            throw exception;
        }
    }

    public void done(long settlementExecutionId) {
        settlementExecutionDao.updateDone(settlementExecutionId, fantasyTime.getDateTime());
    }

    public boolean isDone(LocalDate targetDate) {
        return settlementExecutionDao.existsByTargetDateAndStatus(targetDate, SettlementExecutionStatus.DONE);
    }

}
