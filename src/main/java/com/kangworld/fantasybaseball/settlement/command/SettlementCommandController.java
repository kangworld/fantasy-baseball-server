package com.kangworld.fantasybaseball.settlement.command;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.common.api.CommandController;
import com.kangworld.fantasybaseball.settlement.schedule.SettlementScheduler;
import com.kangworld.fantasybaseball.settlement.schedule.SettlementOutcome;

import lombok.RequiredArgsConstructor;

@CommandController
@RequiredArgsConstructor
@RequestMapping("/api/commands/settlements")
public class SettlementCommandController {

    private final SettlementScheduler settlementScheduler;

    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<Void>> runDailySettlement(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate) {
        SettlementOutcome outcome = settlementScheduler.doDailySettlement(targetDate);
        HttpStatus status = switch (outcome) {
            case COMPLETED, ALREADY_DONE -> HttpStatus.OK;
            case ALREADY_RUNNING, BLOCKED_PREVIOUS -> HttpStatus.CONFLICT;
        };
        String message = switch (outcome) {
            case COMPLETED -> "정산을 실행했습니다.";
            case ALREADY_DONE -> "정산이 이미 완료되었습니다.";
            case ALREADY_RUNNING -> "다른 정산이 실행 중입니다.";
            case BLOCKED_PREVIOUS -> "전날 정산이 아직 완료되지 않았습니다.";
        };

        return ResponseEntity.status(status).body(ApiResponse.of(status, message, null));
    }

}
