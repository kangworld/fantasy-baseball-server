package com.kangworld.fantasybaseball.settlement.schedule;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.player.image.service.PlayerImageBatchService;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;
import com.kangworld.fantasybaseball.settlement.service.PlayerBuildSettlementService;
import com.kangworld.fantasybaseball.settlement.service.PlayerCostSettlementService;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementService;
import com.kangworld.fantasybaseball.settlement.service.RankSettlementService;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

    private final PlayerPointSettlementService playerPointSettlementService;
    private final PlayerBuildSettlementService playerBuildSettlementService;
    private final PlayerCostSettlementService playerCostSettlementService;
    private final RankSettlementService rankSettlementService;
    private final SettlementExecutionRecorder settlementExecutionRecorder;
    private final FantasyTime fantasyTime;
    private final PlayerImageBatchService playerImageBatchService;

    private final SettlementMutex settlementMutex;

    @Scheduled(cron = "${settlement.schedule.daily-cron}", zone = "${settlement.schedule.zone}")
    public void doDailySettlement() {
        // 정산 결과는 사용자가 조회할 현재 서비스 날짜에 저장하고, 경기 기록은 전날 경기에서 가져온다.
        doDailySettlement(fantasyTime.rawNow());
    }

    public SettlementOutcome doDailySettlement(LocalDate settlementDate) {
        return settlementMutex.run(() -> settle(settlementDate));
    }

    private SettlementOutcome settle(LocalDate settlementDate) {
        if (settlementExecutionRecorder.isDone(settlementDate))
            return SettlementOutcome.ALREADY_DONE;
        if (!settlementExecutionRecorder.canStart(settlementDate))
            return SettlementOutcome.BLOCKED_PREVIOUS;

        settlementExecutionRecorder.closeInterrupted(settlementDate);

        LocalDate gameDate = settlementDate.minusDays(1);
        long settlementExecutionId = settlementExecutionRecorder.start(settlementDate);
        settlementExecutionRecorder.runStep(
                settlementExecutionId,
                SettlementStepName.PLAYER_BUILD,
                () -> playerBuildSettlementService.settle(gameDate)
        );
        settlementExecutionRecorder.runStep(
                settlementExecutionId,
                SettlementStepName.PLAYER_POINT,
                () -> playerPointSettlementService.settle(settlementDate, gameDate)
        );
        settlementExecutionRecorder.runStep(
                settlementExecutionId,
                SettlementStepName.PLAYER_COST,
                () -> playerCostSettlementService.settle(settlementDate)
        );
        settlementExecutionRecorder.runStep(
                settlementExecutionId,
                SettlementStepName.DAILY_RANK,
                () -> rankSettlementService.settleDaily(settlementDate, gameDate)
        );
        settlementExecutionRecorder.runStep(
                settlementExecutionId,
                SettlementStepName.SEASON_RANK,
                () -> rankSettlementService.settleSeason(settlementDate)
        );
        settlementExecutionRecorder.done(settlementExecutionId);

        refreshPlayerImages(settlementDate);

        return SettlementOutcome.COMPLETED;
    }

    private void refreshPlayerImages(LocalDate targetDate) {
        try {
            // 선수 이미지는 화면 보조 데이터라 실패해도 포인트/랭킹 정산 완료를 막지 않는다.
            playerImageBatchService.refresh(targetDate);
        } catch (RuntimeException exception) {
            log.warn("player image refresh failed. targetDate={}", targetDate, exception);
        }
    }

}
