package com.kangworld.fantasybaseball.settlement.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import com.kangworld.fantasybaseball.settlement.schedule.SettlementScheduler;
import com.kangworld.fantasybaseball.settlement.schedule.SettlementOutcome;

@ExtendWith(MockitoExtension.class)
class SettlementCommandControllerTest {

    @Mock
    private SettlementScheduler settlementScheduler;

    @InjectMocks
    private SettlementCommandController settlementCommandController;

    @Test
    void runDailySettlement는_이미_완료된_날짜면_정산을_실행하지_않고_완료_메시지를_반환한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 2);
        given(settlementScheduler.doDailySettlement(targetDate)).willReturn(SettlementOutcome.ALREADY_DONE);

        // when
        var response = settlementCommandController.runDailySettlement(targetDate);

        // then
        assertThat(response.getBody().getMessage()).isEqualTo("정산이 이미 완료되었습니다.");
    }

    @Test
    void runDailySettlement는_완료되지_않은_날짜를_수동_정산으로_위임한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 2);
        given(settlementScheduler.doDailySettlement(targetDate)).willReturn(SettlementOutcome.COMPLETED);

        // when
        var response = settlementCommandController.runDailySettlement(targetDate);

        // then
        assertThat(response.getBody().getMessage()).isEqualTo("정산을 실행했습니다.");
        verify(settlementScheduler).doDailySettlement(targetDate);
    }

    /** 다른 정산이 실행 중일 때와 선행 정산이 끝나지 않았을 때 각각의 충돌 응답을 반환한다. */
    @Test
    void returnsConflictWhileAnotherSettlementRunsOrPreviousDayIsIncomplete() {
        LocalDate targetDate = LocalDate.of(2026, 6, 2);
        given(settlementScheduler.doDailySettlement(targetDate))
                .willReturn(SettlementOutcome.ALREADY_RUNNING, SettlementOutcome.BLOCKED_PREVIOUS);

        var running = settlementCommandController.runDailySettlement(targetDate);
        var blocked = settlementCommandController.runDailySettlement(targetDate);

        assertThat(running.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(running.getBody().getMessage()).isEqualTo("다른 정산이 실행 중입니다.");
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(blocked.getBody().getMessage()).isEqualTo("전날 정산이 아직 완료되지 않았습니다.");
    }

}
