package com.kangworld.fantasybaseball.roster.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.exception.RosterVersionConflictException;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPlayerRequest;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPlayerResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterSubmitRequest;
import com.kangworld.fantasybaseball.support.MySqlContainerIntegrationTest;
import com.kangworld.rediscore.lock.RedisLock;
import com.kangworld.rediscore.lock.RedisLockManager;

@MySqlContainerIntegrationTest
class RosterVersionIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 8);
    private static final long MEMBER_ID = 97;

    @Autowired private RosterService rosterService;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private Clock clock;
    @MockitoBean private KboScheduleFetchService schedules;
    @MockitoBean private RedisLockManager lockManager;
    @MockitoSpyBean private RosterPlayerDao rosterPlayers;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2026-06-08T03:00:00Z"));
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
        given(schedules.fetchFresh(DATE)).willReturn(List.of());
        given(lockManager.acquire(eq("roster"), eq(3L), any())).willReturn(new RedisLock("test-roster", "token"));
    }

    /** 같은 버전을 가진 두 기기가 수정해도 먼저 저장한 라인업만 남아야 한다. */
    @Test
    void persistsOnlyTheWinningLineupWhenBothDevicesSendTheSameVersion() {
        List<RosterPlayerRequest> original = lineup(1000);
        List<RosterPlayerRequest> changed = lineup(2000);
        rosterService.submit(MEMBER_ID, new RosterSubmitRequest(original));
        long version = rosterService.find(MEMBER_ID, DATE).getVersion();

        var winning = rosterService.update(MEMBER_ID, new RosterSubmitRequest(changed, version));
        assertThatThrownBy(() -> rosterService.update(MEMBER_ID, new RosterSubmitRequest(original, version)))
                .isInstanceOf(RosterVersionConflictException.class);

        var saved = rosterService.find(MEMBER_ID, DATE);
        assertThat(saved.getVersion()).isEqualTo(winning.getVersion());
        assertThat(saved.getPlayers()).extracting(RosterPlayerResponse::getPlayerId)
                .containsExactlyElementsOf(changed.stream().map(RosterPlayerRequest::getPlayerId).toList());

        assertThat(rosterService.update(MEMBER_ID, new RosterSubmitRequest(original, saved.getVersion())).getVersion())
                .isEqualTo(version + 2);
    }

    /** 선수 목록 저장에 실패하면 앞서 바꾼 버전과 기존 로스터 삭제도 되돌려야 한다. */
    @Test
    void rollsBackVersionAndPlayersWhenReplacingTheLineupFails() {
        long memberId = 98;
        List<RosterPlayerRequest> original = lineup(3000);
        List<RosterPlayerRequest> changed = lineup(4000);
        long version = rosterService.submit(memberId, new RosterSubmitRequest(original)).getVersion();
        doThrow(new IllegalStateException("lineup insert failed")).when(rosterPlayers).insertAll(anyList());

        assertThatThrownBy(() -> rosterService.update(memberId, new RosterSubmitRequest(changed, version)))
                .hasMessage("lineup insert failed");

        var saved = rosterService.find(memberId, DATE);
        assertThat(saved.getVersion()).isEqualTo(version);
        assertThat(saved.getPlayers()).extracting(RosterPlayerResponse::getPlayerId)
                .containsExactlyElementsOf(original.stream().map(RosterPlayerRequest::getPlayerId).toList());
    }

    private List<RosterPlayerRequest> lineup(long firstId) {
        List<PlayerPosition> positions = List.of(
                PlayerPosition.P, PlayerPosition.P, PlayerPosition.P, PlayerPosition.P, PlayerPosition.P,
                PlayerPosition.C, PlayerPosition.B1, PlayerPosition.B2, PlayerPosition.B3, PlayerPosition.SS,
                PlayerPosition.LF, PlayerPosition.CF, PlayerPosition.RF, PlayerPosition.DH);

        return IntStream.range(0, positions.size()).mapToObj(index -> {
            PlayerPosition position = positions.get(index);
            boolean pitcher = position == PlayerPosition.P;
            long id = firstId + index;
            jdbc.update("""
                    insert into player (player_id, player_name, player_type, position, team_code, status,
                                        team_updated_at, created_at, updated_at)
                    values (?, ?, ?, ?, 'LG', 'ACTIVE', now(), now(), now())
                    """, id, "선수" + id, pitcher ? "PITCHER" : "BATTER", position.name());
            jdbc.update("""
                    insert into player_profile (date_id, player_id, cost, point, avg_point, created_at, updated_at)
                    values (?, ?, 1, 0, 0, now(), now())
                    """, DateIds.from(DATE), id);

            return new RosterPlayerRequest(position, pitcher ? index + 1 : 1, id);
        }).toList();
    }
}
