package com.kangworld.fantasybaseball.settlement.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.settlement.schedule.SettlementScheduler;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;

@ExtendWith(MockitoExtension.class)
class SettlementCommandControllerTest {

    @Mock
    private SettlementScheduler settlementScheduler;

    @Mock
    private SettlementExecutionRecorder settlementExecutionRecorder;

    @InjectMocks
    private SettlementCommandController settlementCommandController;

    @Test
    void runDailySettlement는_이미_완료된_날짜면_정산을_실행하지_않고_완료_메시지를_반환한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 2);
        given(settlementExecutionRecorder.isDone(targetDate)).willReturn(true);

        // when
        ApiResponse<Void> response = settlementCommandController.runDailySettlement(targetDate);

        // then
        assertThat(response.getMessage()).isEqualTo("정산이 이미 완료되었습니다.");
        verify(settlementScheduler, never()).doDailySettlement(targetDate);
    }

    @Test
    void runDailySettlement는_완료되지_않은_날짜를_수동_정산으로_위임한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 2);
        given(settlementExecutionRecorder.isDone(targetDate)).willReturn(false);

        // when
        ApiResponse<Void> response = settlementCommandController.runDailySettlement(targetDate);

        // then
        assertThat(response.getMessage()).isEqualTo("정산을 실행했습니다.");
        verify(settlementScheduler).doDailySettlement(targetDate);
    }

}
