package com.kangworld.fantasybaseball.settlement.command;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.common.api.CommandController;
import com.kangworld.fantasybaseball.settlement.schedule.SettlementScheduler;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;

import lombok.RequiredArgsConstructor;

@CommandController
@RequiredArgsConstructor
@RequestMapping("/api/commands/settlements")
public class SettlementCommandController {

    private final SettlementScheduler settlementScheduler;
    private final SettlementExecutionRecorder settlementExecutionRecorder;

    @PostMapping("/daily")
    public ApiResponse<Void> runDailySettlement(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate) {
        // 이미 완료된 날짜는 재정산하지 않는다. 재정산 정책은 아직 열지 않았으므로 명시 메시지만 반환한다.
        LocalDate settlementDate = targetDate;
        if (settlementExecutionRecorder.isDone(settlementDate))
            return ApiResponse.of(HttpStatus.OK, "정산이 이미 완료되었습니다.", null);

        settlementScheduler.doDailySettlement(settlementDate);

        return ApiResponse.of(HttpStatus.OK, "정산을 실행했습니다.", null);
    }

}
