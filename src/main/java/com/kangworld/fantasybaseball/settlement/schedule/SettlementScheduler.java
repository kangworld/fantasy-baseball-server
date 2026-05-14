package com.kangworld.fantasybaseball.settlement.schedule;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementService;
import com.kangworld.fantasybaseball.settlement.service.RankSettlementService;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;
import com.kangworld.rediscore.lock.annotation.RedisLock;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final PlayerPointSettlementService playerPointSettlementService;
    private final RankSettlementService rankSettlementService;
    private final SettlementExecutionRecorder settlementExecutionRecorder;
    private final FantasyTime fantasyTime;

    @Scheduled(cron = "${settlement.schedule.daily-cron}", zone = "${settlement.schedule.zone}")
    @RedisLock(name = "settlement", key = "'daily'")
    public void doDailySettlement() {
        // 02:00 실행 시점은 아직 전날 판타지 날짜라 FantasyTime.now()를 그대로 정산 대상으로 쓴다.
        LocalDate targetDate = fantasyTime.now();
        if (!settlementExecutionRecorder.canStart(targetDate))
            return;

        long settlementExecutionId = settlementExecutionRecorder.start(targetDate);
        settlementExecutionRecorder.runStep(
                settlementExecutionId,
                SettlementStepName.PLAYER_POINT,
                () -> playerPointSettlementService.settle(targetDate)
        );
        settlementExecutionRecorder.runStep(
                settlementExecutionId,
                SettlementStepName.DAILY_RANK,
                () -> rankSettlementService.settleDaily(targetDate)
        );
        settlementExecutionRecorder.runStep(
                settlementExecutionId,
                SettlementStepName.SEASON_RANK,
                () -> rankSettlementService.settleSeason(targetDate)
        );
        settlementExecutionRecorder.done(settlementExecutionId);
    }

}
