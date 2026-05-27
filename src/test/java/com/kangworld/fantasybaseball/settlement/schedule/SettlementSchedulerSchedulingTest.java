package com.kangworld.fantasybaseball.settlement.schedule;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.player.image.service.PlayerImageBatchService;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;
import com.kangworld.fantasybaseball.settlement.service.PlayerCostSettlementService;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementService;
import com.kangworld.fantasybaseball.settlement.service.RankSettlementService;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;
import com.kangworld.rediscore.lock.RedisLock;
import com.kangworld.rediscore.lock.RedisLockManager;

@SpringBootTest(properties = {
        "settlement.schedule.daily-cron=*/1 * * * * *",
        "settlement.schedule.zone=Asia/Seoul"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SettlementSchedulerSchedulingTest {

    @MockitoBean
    private PlayerPointSettlementService playerPointSettlementService;

    @MockitoBean
    private PlayerCostSettlementService playerCostSettlementService;

    @MockitoBean
    private RankSettlementService rankSettlementService;

    @MockitoBean
    private SettlementExecutionRecorder settlementExecutionRecorder;

    @MockitoBean
    private FantasyTime fantasyTime;

    @MockitoBean
    private PlayerImageBatchService playerImageBatchService;

    @MockitoBean
    private RedisLockManager redisLockManager;

    @Test
    void 정산_스케줄러는_프로퍼티로_주입한_cron에_따라_실행된다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 11);
        given(fantasyTime.now()).willReturn(targetDate);
        given(settlementExecutionRecorder.canStart(targetDate)).willReturn(true);
        given(settlementExecutionRecorder.start(targetDate)).willReturn(1L);
        given(redisLockManager.acquire(eq("settlement"), anyLong(), eq("daily")))
                .willReturn(new RedisLock("fantasy-baseball::settlement-daily", "token"));

        // when then
        await()
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(settlementExecutionRecorder, atLeastOnce())
                            .runStep(eq(1L), eq(SettlementStepName.PLAYER_POINT), any(Runnable.class));
                    verify(settlementExecutionRecorder, atLeastOnce())
                            .runStep(eq(1L), eq(SettlementStepName.PLAYER_COST), any(Runnable.class));
                    verify(settlementExecutionRecorder, atLeastOnce())
                            .runStep(eq(1L), eq(SettlementStepName.DAILY_RANK), any(Runnable.class));
                    verify(settlementExecutionRecorder, atLeastOnce())
                            .runStep(eq(1L), eq(SettlementStepName.SEASON_RANK), any(Runnable.class));
                    verify(settlementExecutionRecorder, atLeastOnce()).done(1L);
                });
    }

    @Test
    void 전날_정산이_DONE이_아니면_당일_정산을_시작하지_않는다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 11);
        given(fantasyTime.now()).willReturn(targetDate);
        given(settlementExecutionRecorder.canStart(targetDate)).willReturn(false);

        // when
        new SettlementScheduler(
                playerPointSettlementService,
                playerCostSettlementService,
                rankSettlementService,
                settlementExecutionRecorder,
                fantasyTime,
                playerImageBatchService
        ).doDailySettlement();

        // then
        verify(settlementExecutionRecorder, never()).start(targetDate);
    }

}
