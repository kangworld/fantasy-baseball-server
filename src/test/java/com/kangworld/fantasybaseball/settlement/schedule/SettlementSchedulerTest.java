package com.kangworld.fantasybaseball.settlement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementService;
import com.kangworld.fantasybaseball.settlement.service.RankSettlementService;
import com.kangworld.rediscore.lock.annotation.RedisLock;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @Mock
    private PlayerPointSettlementService playerPointSettlementService;

    @Mock
    private RankSettlementService rankSettlementService;

    @Mock
    private FantasyTime fantasyTime;

    @InjectMocks
    private SettlementScheduler settlementScheduler;

    @Test
    void doDailySettlement는_FantasyTime_now를_정산_대상_날짜로_사용한다() {
        // given
        LocalDate fantasyDate = LocalDate.of(2026, 5, 11);
        given(fantasyTime.now()).willReturn(fantasyDate);

        // when
        settlementScheduler.doDailySettlement();

        // then
        InOrder inOrder = inOrder(playerPointSettlementService, rankSettlementService);
        inOrder.verify(playerPointSettlementService).settle(fantasyDate);
        inOrder.verify(rankSettlementService).settle(fantasyDate);
    }

    @Test
    void doDailySettlement는_새벽_2시_스케줄과_고정_락을_사용한다() throws NoSuchMethodException {
        // when
        Scheduled scheduled = SettlementScheduler.class.getMethod("doDailySettlement").getAnnotation(Scheduled.class);
        RedisLock redisLock = SettlementScheduler.class.getMethod("doDailySettlement").getAnnotation(RedisLock.class);

        // then
        assertThat(scheduled.cron()).isEqualTo("0 0 2 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
        assertThat(redisLock.name()).isEqualTo("settlement");
        assertThat(redisLock.key()).isEqualTo("'daily'");
    }

}
