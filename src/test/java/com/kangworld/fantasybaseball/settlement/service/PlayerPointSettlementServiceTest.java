package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
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
    private KboScheduleFetchService scheduleFetchService;

    @Mock
    private KboGameRecordFetchService gameRecordFetchService;

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

    @InjectMocks
    private PlayerPointSettlementService playerPointSettlementService;

    @Test
    void settle은_FINISHED와_SUSPENDED_경기만_기록을_조회해_선수_포인트를_저장한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 11);
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
        given(scheduleFetchService.fetch(targetDate)).willReturn(List.of(
                game("game-1", KboGameStatus.PLAYING),
                game("game-2", KboGameStatus.FINISHED),
                game("game-3", KboGameStatus.SUSPENDED),
                game("game-4", KboGameStatus.CANCELED)
        ));
        given(gameRecordFetchService.fetch("game-2")).willReturn(finishedRecord);
        given(gameRecordFetchService.fetch("game-3")).willReturn(suspendedRecord);
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
        given(playerProfilePointDao.findByDateId(20260510)).willReturn(List.of(
                profile(100L, 10, 2),
                profile(200L, 30, 3)
        ));

        // when
        playerPointSettlementService.settle(targetDate);

        // then
        ArgumentCaptor<List<PlayerDailyPoint>> dailyPointsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<PlayerProfile>> profilesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerDailyPointDao).upsertAll(dailyPointsCaptor.capture());
        verify(playerProfilePointDao).upsertAll(profilesCaptor.capture());

        assertThat(dailyPointsCaptor.getValue()).extracting(PlayerDailyPoint::getDateId)
                .containsExactly(20260511);
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
        verify(gameRecordFetchService).fetch("game-2");
        verify(gameRecordFetchService).fetch("game-3");
        verifyNoMoreInteractions(gameRecordFetchService);
    }

    private KboScheduleGame game(String gameId, KboGameStatus status) {
        return KboScheduleGame.builder()
                .gameId(gameId)
                .status(status)
                .build();
    }

    private KboGameRecord gameRecord(String gameId) {
        return KboGameRecord.builder()
                .gameId(gameId)
                .build();
    }

    private PlayerProfile profile(long playerId, int point, int pointGameCount) {
        return PlayerProfile.builder()
                .dateId(20260510)
                .playerId(playerId)
                .teamCode("LG")
                .pos("P")
                .cost(10)
                .point(point)
                .avgPoint(point / pointGameCount)
                .pointGameCount(pointGameCount)
                .build();
    }

}
