package com.kangworld.fantasybaseball.settlement.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementExecution {

    private long settlementExecutionId;
    private LocalDate targetDate;
    private SettlementExecutionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private SettlementStepName failureStep;
    private String failureMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
