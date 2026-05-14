package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.settlement.dao.SettlementExecutionDao;
import com.kangworld.fantasybaseball.settlement.dao.SettlementStepExecutionDao;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecution;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecutionStatus;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;

@ExtendWith(MockitoExtension.class)
class SettlementExecutionRecorderTest {

    @Mock
    private SettlementExecutionDao settlementExecutionDao;

    @Mock
    private SettlementStepExecutionDao settlementStepExecutionDao;

    @Mock
    private FantasyTime fantasyTime;

    private SettlementExecutionRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new SettlementExecutionRecorder(settlementExecutionDao, settlementStepExecutionDao, fantasyTime);
    }

    @Test
    void canStart는_전날_정산이_DONE이면_true를_반환한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 12);
        given(settlementExecutionDao.findLatestByTargetDate(LocalDate.of(2026, 5, 11)))
                .willReturn(Optional.of(execution(SettlementExecutionStatus.DONE)));

        // when then
        org.assertj.core.api.Assertions.assertThat(recorder.canStart(targetDate)).isTrue();
    }

    @Test
    void canStart는_전날_정산이_FAILED이면_false를_반환한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 12);
        given(settlementExecutionDao.findLatestByTargetDate(LocalDate.of(2026, 5, 11)))
                .willReturn(Optional.of(execution(SettlementExecutionStatus.FAILED)));

        // when then
        org.assertj.core.api.Assertions.assertThat(recorder.canStart(targetDate)).isFalse();
    }

    @Test
    void runStep은_단계가_성공하면_단계_DONE을_기록한다() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 12, 2, 0);
        given(fantasyTime.getDateTime()).willReturn(now);
        given(settlementStepExecutionDao.insertStarted(1L, SettlementStepName.PLAYER_POINT, now)).willReturn(10L);

        // when
        recorder.runStep(1L, SettlementStepName.PLAYER_POINT, () -> {
        });

        // then
        verify(settlementStepExecutionDao).updateDone(10L, now);
        verify(settlementExecutionDao, never()).updateFailed(1L, SettlementStepName.PLAYER_POINT, null, now);
    }

    @Test
    void runStep은_단계가_실패하면_단계와_전체_실패를_기록하고_예외를_다시_던진다() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 12, 2, 0);
        IllegalStateException exception = new IllegalStateException("daily rank failed");
        given(fantasyTime.getDateTime()).willReturn(now);
        given(settlementStepExecutionDao.insertStarted(1L, SettlementStepName.DAILY_RANK, now)).willReturn(10L);

        // when then
        assertThatThrownBy(() -> recorder.runStep(1L, SettlementStepName.DAILY_RANK, () -> {
            throw exception;
        })).isSameAs(exception);
        verify(settlementStepExecutionDao).updateFailed(
                10L,
                IllegalStateException.class.getName(),
                "daily rank failed",
                now
        );
        verify(settlementExecutionDao).updateFailed(
                1L,
                SettlementStepName.DAILY_RANK,
                "daily rank failed",
                now
        );
    }

    private SettlementExecution execution(SettlementExecutionStatus status) {
        return SettlementExecution.builder()
                .status(status)
                .build();
    }

}
