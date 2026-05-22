package com.kangworld.fantasybaseball.settlement.schedule;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.player.image.service.PlayerImageBatchService;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementResult;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementService;
import com.kangworld.fantasybaseball.settlement.service.RankSettlementService;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;
import com.kangworld.rediscore.lock.annotation.RedisLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

    private final PlayerPointSettlementService playerPointSettlementService;
    private final RankSettlementService rankSettlementService;
    private final SettlementExecutionRecorder settlementExecutionRecorder;
    private final FantasyTime fantasyTime;
    private final PlayerImageBatchService playerImageBatchService;

    @Scheduled(cron = "${settlement.schedule.daily-cron}", zone = "${settlement.schedule.zone}")
    @RedisLock(name = "settlement", key = "'daily'")
    public void doDailySettlement() {
        // 02:00 실행 시점은 아직 전날 판타지 날짜라 FantasyTime.now()를 그대로 정산 대상으로 쓴다.
        LocalDate targetDate = fantasyTime.now();
        if (!settlementExecutionRecorder.canStart(targetDate))
            return;

        long settlementExecutionId = settlementExecutionRecorder.start(targetDate);
        PlayerPointSettlementResult playerPointSettlementResult = settlementExecutionRecorder.runStepWithResult(
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

        refreshPlayerImages(targetDate, playerPointSettlementResult);
    }

    private void refreshPlayerImages(LocalDate targetDate, PlayerPointSettlementResult playerPointSettlementResult) {
        try {
            // 선수 이미지는 화면 보조 데이터라 실패해도 포인트/랭킹 정산 완료를 막지 않는다.
            playerImageBatchService.refresh(targetDate, playerPointSettlementResult);
        } catch (RuntimeException exception) {
            log.warn("player image refresh failed. targetDate={}", targetDate, exception);
        }
    }

}
