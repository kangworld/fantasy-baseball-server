package com.kangworld.fantasybaseball.settlement.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementStepExecution {

    private long settlementStepExecutionId;
    private long settlementExecutionId;
    private SettlementStepName stepName;
    private SettlementStepStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String failureType;
    private String failureMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
