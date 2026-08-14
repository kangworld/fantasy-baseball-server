package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;
import com.kangworld.fantasybaseball.point.dao.PointRuleDao;
import com.kangworld.fantasybaseball.point.model.BatterGameRecord;
import com.kangworld.fantasybaseball.point.model.CalculatedPlayerPoint;
import com.kangworld.fantasybaseball.point.model.PitcherGameRecord;
import com.kangworld.fantasybaseball.point.model.PlayerDailyPoint;
import com.kangworld.fantasybaseball.point.service.PlayerGameRecordMapper;
import com.kangworld.fantasybaseball.point.service.PointCalculator;

@ExtendWith(MockitoExtension.class)
class PlayerPointSettlementServiceTest {

    @Mock
    private SettlementGameRecordService settlementGameRecordService;

    @Mock
    private PointRuleDao pointRuleDao;

    @Mock
    private PlayerDailyPointDao playerDailyPointDao;

    @Mock
    private PlayerProfilePointDao playerProfilePointDao;

    @Mock
    private PlayerGameRecordMapper playerGameRecordMapper;

    @Mock
    private PointCalculator pointCalculator;

    @Mock
    private FantasyTime fantasyTime;

    @Spy
    private SettlementTransactionExecutor transactionExecutor = new SettlementTransactionExecutor();

    @InjectMocks
    private PlayerPointSettlementService playerPointSettlementService;

    /** 기록 조회가 실패하면 현재 날짜의 선수 점수와 프로필을 쓰지 않아야 한다. */
    @Test
    void 경기_기록_조회가_실패하면_점수와_프로필을_쓰지_않는다() {
        LocalDate gameDate = LocalDate.of(2026, 5, 11);
        given(settlementGameRecordService.fetch(gameDate)).willThrow(new IllegalStateException("기록 누락"));

        assertThatThrownBy(() -> playerPointSettlementService.settle(gameDate.plusDays(1), gameDate))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(playerDailyPointDao, playerProfilePointDao);
    }

    /** 정산에 사용한 기록이 선수별 포인트로 저장되어야 한다. */
    @Test
    void settle은_정산용_경기_기록의_선수_포인트를_저장한다() {
        // given
        LocalDate settlementDate = LocalDate.of(2026, 5, 12);
        LocalDate gameDate = LocalDate.of(2026, 5, 11);
        LocalDateTime now = LocalDateTime.of(2026, 5, 12, 2, 0);
        KboGameRecord finishedRecord = gameRecord("game-2");
        KboGameRecord suspendedRecord = gameRecord("game-3");
        BatterGameRecord batterRecord = BatterGameRecord.builder()
                .gameId("game-2")
                .playerId(100L)
                .build();
        PitcherGameRecord pitcherRecord = PitcherGameRecord.builder()
                .gameId("game-2")
                .playerId(100L)
                .build();
        given(settlementGameRecordService.fetch(gameDate)).willReturn(List.of(finishedRecord, suspendedRecord));
        given(fantasyTime.getDateTime()).willReturn(now);
        given(pointRuleDao.findAllActive()).willReturn(List.of());
        given(playerGameRecordMapper.toBatterRecords(finishedRecord)).willReturn(List.of(batterRecord));
        given(playerGameRecordMapper.toPitcherRecords(finishedRecord)).willReturn(List.of(pitcherRecord));
        given(playerGameRecordMapper.toBatterRecords(suspendedRecord)).willReturn(List.of());
        given(playerGameRecordMapper.toPitcherRecords(suspendedRecord)).willReturn(List.of());
        given(pointCalculator.calculateBatter(batterRecord, List.of()))
                .willReturn(new CalculatedPlayerPoint("game-2", 100L, 5));
        given(pointCalculator.calculatePitcher(pitcherRecord, List.of()))
                .willReturn(new CalculatedPlayerPoint("game-2", 100L, 2));
        given(playerProfilePointDao.findByDateId(20260511)).willReturn(List.of(
                profile(100L, 10, 2),
                profile(200L, 30, 3)
        ));
        // when
        playerPointSettlementService.settle(settlementDate, gameDate);

        // then
        ArgumentCaptor<List<PlayerDailyPoint>> dailyPointsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<PlayerProfile>> profilesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerDailyPointDao).upsertAll(dailyPointsCaptor.capture());
        verify(playerProfilePointDao).insertAll(profilesCaptor.capture());

        assertThat(dailyPointsCaptor.getValue()).extracting(PlayerDailyPoint::getDateId)
                .containsExactly(20260512);
        assertThat(dailyPointsCaptor.getValue()).extracting(PlayerDailyPoint::getGameId)
                .containsExactly("game-2");
        assertThat(dailyPointsCaptor.getValue()).extracting(PlayerDailyPoint::getPlayerId)
                .containsExactly(100L);
        assertThat(dailyPointsCaptor.getValue()).extracting(PlayerDailyPoint::getPoint)
                .containsExactly(7);
        assertThat(profilesCaptor.getValue()).extracting(PlayerProfile::getPlayerId)
                .containsExactly(100L, 200L);
        assertThat(profilesCaptor.getValue()).extracting(PlayerProfile::getPoint)
                .containsExactly(17, 30);
        assertThat(profilesCaptor.getValue()).extracting(PlayerProfile::getPointGameCount)
                .containsExactly(3, 3);
    }

    private KboGameRecord gameRecord(String gameId) {
        return KboGameRecord.builder()
                .gameId(gameId)
                .batterRecords(List.of(KboBatterRecord.builder()
                        .playerCode("100")
                        .name("타자")
                        .teamCode("LG")
                        .position(PlayerPosition.DH)
                        .build()))
                .pitcherRecords(List.of(KboPitcherRecord.builder()
                        .playerCode("100")
                        .name("투수")
                        .teamCode("LG")
                        .position(PlayerPosition.P)
                        .build()))
                .build();
    }

    private PlayerProfile profile(long playerId, int point, int pointGameCount) {
        return PlayerProfile.builder()
                .dateId(20260511)
                .playerId(playerId)
                .cost(10)
                .point(point)
                .avgPoint(point / pointGameCount)
                .pointGameCount(pointGameCount)
                .build();
    }

}
