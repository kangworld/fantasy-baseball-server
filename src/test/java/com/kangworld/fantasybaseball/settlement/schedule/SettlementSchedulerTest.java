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
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.player.image.service.PlayerImageBatchService;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementResult;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementService;
import com.kangworld.fantasybaseball.settlement.service.RankSettlementService;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;
import com.kangworld.rediscore.lock.annotation.RedisLock;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @Mock
    private PlayerPointSettlementService playerPointSettlementService;

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
    void doDailySettlement는_FantasyTime_now를_정산_대상_날짜로_사용한다() {
        // given
        LocalDate fantasyDate = LocalDate.of(2026, 5, 11);
        PlayerPointSettlementResult settlementResult = emptySettlementResult();
        given(fantasyTime.now()).willReturn(fantasyDate);
        given(settlementExecutionRecorder.canStart(fantasyDate)).willReturn(true);
        given(settlementExecutionRecorder.start(fantasyDate)).willReturn(1L);
        given(settlementExecutionRecorder.runStepWithResult(
                eq(1L),
                eq(SettlementStepName.PLAYER_POINT),
                org.mockito.ArgumentMatchers.<Supplier<PlayerPointSettlementResult>>any()
        )).willReturn(settlementResult);

        // when
        settlementScheduler.doDailySettlement();

        // then
        InOrder inOrder = inOrder(settlementExecutionRecorder);
        inOrder.verify(settlementExecutionRecorder).canStart(fantasyDate);
        inOrder.verify(settlementExecutionRecorder).start(fantasyDate);
        inOrder.verify(settlementExecutionRecorder)
                .runStepWithResult(eq(1L), eq(SettlementStepName.PLAYER_POINT), org.mockito.ArgumentMatchers.<Supplier<PlayerPointSettlementResult>>any());
        inOrder.verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.DAILY_RANK), any(Runnable.class));
        inOrder.verify(settlementExecutionRecorder)
                .runStep(eq(1L), eq(SettlementStepName.SEASON_RANK), any(Runnable.class));
        inOrder.verify(settlementExecutionRecorder).done(1L);
        verify(playerImageBatchService).refresh(fantasyDate, settlementResult);
    }

    @Test
    void doDailySettlement는_전날_정산이_DONE이_아니면_시작하지_않는다() {
        // given
        LocalDate fantasyDate = LocalDate.of(2026, 5, 11);
        given(fantasyTime.now()).willReturn(fantasyDate);
        given(settlementExecutionRecorder.canStart(fantasyDate)).willReturn(false);

        // when
        settlementScheduler.doDailySettlement();

        // then
        verify(settlementExecutionRecorder, never()).start(fantasyDate);
    }

    @Test
    void doDailySettlement는_이미지_갱신이_실패해도_랭킹_정산을_계속한다() {
        // given
        LocalDate fantasyDate = LocalDate.of(2026, 5, 11);
        PlayerPointSettlementResult settlementResult = emptySettlementResult();
        given(fantasyTime.now()).willReturn(fantasyDate);
        given(settlementExecutionRecorder.canStart(fantasyDate)).willReturn(true);
        given(settlementExecutionRecorder.start(fantasyDate)).willReturn(1L);
        given(settlementExecutionRecorder.runStepWithResult(
                eq(1L),
                eq(SettlementStepName.PLAYER_POINT),
                org.mockito.ArgumentMatchers.<Supplier<PlayerPointSettlementResult>>any()
        )).willReturn(settlementResult);
        willThrow(new IllegalStateException("s3 failure"))
                .given(playerImageBatchService).refresh(fantasyDate, settlementResult);

        // when
        settlementScheduler.doDailySettlement();

        // then
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

        // then
        assertThat(scheduled.cron()).isEqualTo("${settlement.schedule.daily-cron}");
        assertThat(scheduled.zone()).isEqualTo("${settlement.schedule.zone}");
        assertThat(redisLock.name()).isEqualTo("settlement");
        assertThat(redisLock.key()).isEqualTo("'daily'");
    }

    private PlayerPointSettlementResult emptySettlementResult() {
        return new PlayerPointSettlementResult(List.of(), List.of());
    }

}
