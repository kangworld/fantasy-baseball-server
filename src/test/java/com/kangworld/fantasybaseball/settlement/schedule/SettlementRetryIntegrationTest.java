package com.kangworld.fantasybaseball.settlement.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.player.image.service.PlayerImageBatchService;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;
import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.settlement.dao.SettlementExecutionDao;
import com.kangworld.fantasybaseball.settlement.model.SettlementExecutionStatus;
import com.kangworld.fantasybaseball.settlement.model.SettlementStepName;
import com.kangworld.fantasybaseball.settlement.service.RankSettlementService;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;
import com.kangworld.fantasybaseball.settlement.service.SettlementGameRecordService;
import com.kangworld.fantasybaseball.support.MySqlContainerIntegrationTest;

@MySqlContainerIntegrationTest
class SettlementRetryIntegrationTest {

    @Autowired private SettlementScheduler scheduler;
    @Autowired private SettlementMutex mutex;
    @Autowired private SettlementExecutionRecorder recorder;
    @Autowired private SettlementExecutionDao executionDao;
    @Autowired private PlayerProfilePointDao profiles;
    @Autowired private PlayerDailyPointDao points;
    @Autowired private DailyRankDao dailyRanks;
    @Autowired private SeasonRankDao seasonRanks;
    @Autowired private RosterDao rosters;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private SettlementGameRecordService gameRecords;
    @MockitoBean private PlayerImageBatchService images;
    @MockitoSpyBean private RankSettlementService ranks;

    /** 시즌 순위 저장이 실패한 뒤 재실행해도 앞 단계의 결과가 중복되지 않아야 한다. */
    @Test
    void rerunsCommittedStepsAfterSeasonRankFails() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        int dateId = DateIds.from(date);
        LocalDate gameDate = date.minusDays(1);
        int gameDateId = DateIds.from(gameDate);
        profiles.insertAll(List.of(PlayerProfile.builder()
                .dateId(gameDateId).playerId(78).cost(2)
                .createdAt(gameDate.atStartOfDay()).updatedAt(gameDate.atStartOfDay()).build()));
        rosters.insert(Roster.builder().memberId(79).dateId(gameDateId)
                .createdAt(gameDate.atStartOfDay()).updatedAt(gameDate.atStartOfDay()).build());
        KboGameRecord game = KboGameRecord.builder()
                .gameId("20260811LGHH0")
                .batterRecords(List.of(KboBatterRecord.builder()
                        .playerCode("78").name("Player 78").teamCode("LG").position(PlayerPosition.DH).build()))
                .pitcherRecords(List.of())
                .etcRecords(List.of())
                .build();
        given(gameRecords.fetch(gameDate)).willReturn(List.of(game), List.of(game), List.of(), List.of());
        doThrow(new IllegalStateException("season rank unavailable")).doCallRealMethod().when(ranks).settleSeason(date);

        assertThatThrownBy(() -> scheduler.doDailySettlement(date)).hasMessage("season rank unavailable");
        assertThat(profiles.findByDateId(dateId)).hasSize(1);
        assertThat(points.findPointByPlayerIds(dateId, Set.of(78L))).containsKey(78L);
        assertThat(dailyRanks.findByDateId(dateId)).hasSize(1);
        assertThat(seasonRanks.findByDateId(dateId)).isEmpty();
        assertThat(executionDao.findLatestByTargetDate(date).orElseThrow().getStatus())
                .isEqualTo(SettlementExecutionStatus.FAILED);

        assertThat(scheduler.doDailySettlement(date.plusDays(1))).isEqualTo(SettlementOutcome.BLOCKED_PREVIOUS);
        assertThat(scheduler.doDailySettlement(date)).isEqualTo(SettlementOutcome.COMPLETED);
        assertThat(scheduler.doDailySettlement(date)).isEqualTo(SettlementOutcome.ALREADY_DONE);
        assertThat(profiles.findByDateId(dateId)).hasSize(1);
        assertThat(points.findPointByPlayerIds(dateId, Set.of(78L))).isEmpty();
        assertThat(dailyRanks.findByDateId(dateId)).hasSize(1);
        assertThat(seasonRanks.findByDateId(dateId)).hasSize(1);
    }

    /** MySQL 락을 가지고 있을 때만 중단된 정산을 실패로 닫고 재실행한다. */
    @Test
    void recoversAnInterruptedExecutionOnlyAfterAcquiringTheMysqlLock() {
        LocalDate date = LocalDate.of(2026, 8, 16);
        long id = recorder.start(date);
        jdbc.update("""
                insert into settlement_step_execution
                (settlement_execution_id, step_name, status, started_at, created_at, updated_at)
                values (?, ?, 'STARTED', now(), now(), now())
                """, id, SettlementStepName.PLAYER_BUILD.name());

        assertThat(mutex.run(() -> scheduler.doDailySettlement(date))).isEqualTo(SettlementOutcome.ALREADY_RUNNING);
        assertThat(executionDao.findLatestByTargetDate(date).orElseThrow().getStatus())
                .isEqualTo(SettlementExecutionStatus.STARTED);

        assertThat(scheduler.doDailySettlement(date)).isEqualTo(SettlementOutcome.COMPLETED);
        assertThat(jdbc.queryForObject(
                "select status from settlement_execution where settlement_execution_id = ?", String.class, id))
                .isEqualTo("FAILED");
        assertThat(jdbc.queryForObject(
                "select status from settlement_step_execution where settlement_execution_id = ?", String.class, id))
                .isEqualTo("FAILED");
    }
}
