package com.kangworld.fantasybaseball.settlement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.player.image.service.PlayerImageBatchService;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;
import com.kangworld.fantasybaseball.settlement.service.PlayerBuildSettlementService;
import com.kangworld.fantasybaseball.settlement.service.PlayerCostSettlementService;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementService;
import com.kangworld.fantasybaseball.settlement.service.RankSettlementService;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;
import com.kangworld.rediscore.lock.annotation.RedisLock;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @Mock
    private PlayerPointSettlementService playerPointSettlementService;

    @Mock
    private PlayerBuildSettlementService playerBuildSettlementService;

    @Mock
    private PlayerCostSettlementService playerCostSettlementService;

    @Mock
    private RankSettlementService rankSettlementService;

    @Mock
    private SettlementExecutionRecorder settlementExecutionRecorder;

    @Mock
    private FantasyTime fantasyTime;

    @Mock
    private PlayerImageBatchService playerImageBatchService;

    @InjectMocks
    private SettlementScheduler settlementScheduler;

    @Test
    void doDailySettlement는_rawNow를_정산_기준일로_사용한다() {
        // given
        LocalDate settlementDate = LocalDate.of(2026, 5, 12);
        given(fantasyTime.rawNow()).willReturn(settlementDate);
        given(settlementExecutionRecorder.canStart(settlementDate)).willReturn(true);
        given(settlementExecutionRecorder.start(settlementDate)).willReturn(1L);

        // when
        settlementScheduler.doDailySettlement();

        // then
        InOrder inOrder = inOrder(settlementExecutionRecorder);
        inOrder.verify(settlementExecutionRecorder).canStart(settlementDate);
        inOrder.verify(settlementExecutionRecorder).start(settlementDate);
        inOrder.verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.PLAYER_BUILD), any(Runnable.class));
        inOrder.verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.PLAYER_POINT), any(Runnable.class));
        inOrder.verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.PLAYER_COST), any(Runnable.class));
        inOrder.verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.DAILY_RANK), any(Runnable.class));
        inOrder.verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.SEASON_RANK), any(Runnable.class));
        inOrder.verify(settlementExecutionRecorder).done(1L);
        verify(playerImageBatchService).refresh(settlementDate);
    }

    @Test
    void doDailySettlement는_정산일과_경기일을_분리해_스텝을_호출한다() {
        // given
        LocalDate settlementDate = LocalDate.of(2026, 6, 7);
        LocalDate gameDate = LocalDate.of(2026, 6, 6);
        given(settlementExecutionRecorder.canStart(settlementDate)).willReturn(true);
        given(settlementExecutionRecorder.start(settlementDate)).willReturn(1L);

        // when
        settlementScheduler.doDailySettlement(settlementDate);

        // then
        ArgumentCaptor<Runnable> playerBuildStep = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> playerPointStep = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> playerCostStep = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> dailyRankStep = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> seasonRankStep = ArgumentCaptor.forClass(Runnable.class);

        verify(settlementExecutionRecorder).runStep(eq(1L), eq(SettlementStepName.PLAYER_BUILD), playerBuildStep.capture());
        verify(settlementExecutionRecorder).runStep(eq(1L), eq(SettlementStepName.PLAYER_POINT), playerPointStep.capture());
        verify(settlementExecutionRecorder).runStep(eq(1L), eq(SettlementStepName.PLAYER_COST), playerCostStep.capture());
        verify(settlementExecutionRecorder).runStep(eq(1L), eq(SettlementStepName.DAILY_RANK), dailyRankStep.capture());
        verify(settlementExecutionRecorder).runStep(eq(1L), eq(SettlementStepName.SEASON_RANK), seasonRankStep.capture());

        playerBuildStep.getValue().run();
        playerPointStep.getValue().run();
        playerCostStep.getValue().run();
        dailyRankStep.getValue().run();
        seasonRankStep.getValue().run();

        verify(playerBuildSettlementService).settle(gameDate);
        verify(playerPointSettlementService).settle(settlementDate, gameDate);
        verify(playerCostSettlementService).settle(settlementDate);
        verify(rankSettlementService).settleDaily(settlementDate);
        verify(rankSettlementService).settleSeason(settlementDate);
    }

    @Test
    void doDailySettlement는_전날_정산이_DONE이_아니면_시작하지_않는다() {
        // given
        LocalDate settlementDate = LocalDate.of(2026, 5, 11);
        given(fantasyTime.rawNow()).willReturn(settlementDate);
        given(settlementExecutionRecorder.canStart(settlementDate)).willReturn(false);

        // when
        settlementScheduler.doDailySettlement();

        // then
        verify(settlementExecutionRecorder, never()).start(settlementDate);
    }

    @Test
    void doDailySettlement는_이미지_갱신이_실패해도_랭킹_정산을_계속한다() {
        // given
        LocalDate settlementDate = LocalDate.of(2026, 5, 11);
        given(fantasyTime.rawNow()).willReturn(settlementDate);
        given(settlementExecutionRecorder.canStart(settlementDate)).willReturn(true);
        given(settlementExecutionRecorder.start(settlementDate)).willReturn(1L);
        willThrow(new IllegalStateException("s3 failure"))
                .given(playerImageBatchService).refresh(settlementDate);

        // when
        settlementScheduler.doDailySettlement();

        // then
        verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.PLAYER_BUILD), any(Runnable.class));
        verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.PLAYER_COST), any(Runnable.class));
        verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.DAILY_RANK), any(Runnable.class));
        verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.SEASON_RANK), any(Runnable.class));
        verify(settlementExecutionRecorder).done(1L);
    }

    @Test
    void doDailySettlement는_정산_스케줄_프로퍼티와_고정_락을_사용한다() throws NoSuchMethodException {
        // when
        Scheduled scheduled = SettlementScheduler.class.getMethod("doDailySettlement").getAnnotation(Scheduled.class);
        RedisLock redisLock = SettlementScheduler.class.getMethod("doDailySettlement").getAnnotation(RedisLock.class);
        RedisLock manualRedisLock = SettlementScheduler.class.getMethod("doDailySettlement", LocalDate.class)
                .getAnnotation(RedisLock.class);

        // then
        assertThat(scheduled.cron()).isEqualTo("${settlement.schedule.daily-cron}");
        assertThat(scheduled.zone()).isEqualTo("${settlement.schedule.zone}");
        assertThat(redisLock.name()).isEqualTo("settlement");
        assertThat(redisLock.key()).isEqualTo("'daily'");
        assertThat(manualRedisLock.name()).isEqualTo("settlement");
        assertThat(manualRedisLock.key()).isEqualTo("'daily'");
    }

}
