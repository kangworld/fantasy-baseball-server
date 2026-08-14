package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;

@SpringJUnitConfig(SettlementGameRecordService.class)
class SettlementGameRecordServiceTest {

    private static final LocalDate GAME_DATE = LocalDate.of(2026, 5, 18);

    @Autowired
    private SettlementGameRecordService service;

    @MockitoBean
    private KboScheduleFetchService scheduleFetchService;

    @MockitoBean
    private KboGameRecordFetchService gameRecordFetchService;

    /** 경기가 없거나 모두 취소되면 기록 API를 부를 필요가 없다. */
    @Test
    void fetch는_경기가_없는_날과_전부_취소된_날에_빈_결과를_반환한다() {
        given(scheduleFetchService.fetchFresh(GAME_DATE))
                .willReturn(List.of(), List.of(game(KboGameStatus.CANCELED)));

        assertThat(service.fetch(GAME_DATE)).isEmpty();
        assertThat(service.fetch(GAME_DATE)).isEmpty();
        verifyNoInteractions(gameRecordFetchService);
    }

    /** 정산할 수 없는 상태의 경기는 기록을 요청하기 전에 차단해야 한다. */
    @ParameterizedTest
    @EnumSource(value = KboGameStatus.class, names = {"READY", "PLAYING", "UNKNOWN"})
    void fetch는_정산이_가능한_상태가_될_때까지_실패한다(KboGameStatus status) {
        given(scheduleFetchService.fetchFresh(GAME_DATE)).willReturn(List.of(game(status)));

        assertThatThrownBy(() -> service.fetch(GAME_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상태가 확정되지 않았습니다");
        verifyNoInteractions(gameRecordFetchService);
    }

    /** 일정에서 종료된 경기라도 박스스코어가 불완전하면 정산할 수 없다. */
    @Test
    void 종료_일정이라도_기록이_불완전하면_실패한다() {
        given(scheduleFetchService.fetchFresh(GAME_DATE)).willReturn(List.of(game(KboGameStatus.FINISHED)));
        given(gameRecordFetchService.fetchFresh("game-1")).willReturn(record(KboGameStatus.FINISHED, false));

        assertThatThrownBy(() -> service.fetch(GAME_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기록이 아직 준비되지 않았습니다");
    }

    /** 종료 또는 중단 상태가 확인되고 박스스코어가 준비된 경기는 정산에 사용한다. */
    @ParameterizedTest
    @EnumSource(value = KboGameStatus.class, names = {"FINISHED", "SUSPENDED"})
    void 정산_대상_경기의_완전한_기록을_반환한다(KboGameStatus status) {
        KboGameRecord record = record(status, true);
        given(scheduleFetchService.fetchFresh(GAME_DATE)).willReturn(List.of(game(status)));
        given(gameRecordFetchService.fetchFresh("game-1")).willReturn(record);

        assertThat(service.fetch(GAME_DATE)).containsExactly(record);
    }

    /** 일정이 중단 상태여도 기록이 진행 중이거나 대기 중이면 아직 정산할 수 없다. */
    @ParameterizedTest
    @EnumSource(value = KboGameStatus.class, names = {"READY", "PLAYING", "CANCELED", "UNKNOWN"})
    void rejectsAnUnsettledRecordForASuspendedGame(KboGameStatus recordStatus) {
        given(scheduleFetchService.fetchFresh(GAME_DATE)).willReturn(List.of(game(KboGameStatus.SUSPENDED)));
        given(gameRecordFetchService.fetchFresh("game-1")).willReturn(record(recordStatus, true));

        assertThatThrownBy(() -> service.fetch(GAME_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기록이 아직 준비되지 않았습니다");
    }

    private KboScheduleGame game(KboGameStatus status) {
        return KboScheduleGame.builder().gameId("game-1").status(status).build();
    }

    private KboGameRecord record(KboGameStatus status, boolean completeBoxScore) {
        return KboGameRecord.builder()
                .gameId("game-1")
                .status(status)
                .completeBoxScore(completeBoxScore)
                .batterRecords(List.of(KboBatterRecord.builder().playerCode("100").build()))
                .pitcherRecords(List.of(KboPitcherRecord.builder().playerCode("200").build()))
                .build();
    }
}
