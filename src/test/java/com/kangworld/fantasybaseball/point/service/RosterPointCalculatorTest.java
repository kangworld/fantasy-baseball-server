package com.kangworld.fantasybaseball.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;

@ExtendWith(MockitoExtension.class)
class RosterPointCalculatorTest {

    @Mock
    private RosterPlayerDao rosterPlayerDao;

    @Mock
    private PlayerDailyPointDao playerDailyPointDao;

    @InjectMocks
    private RosterPointCalculator rosterPointCalculator;

    @Test
    void calculateAll은_여러_로스터의_선수_점수를_벌크로_조회해_로스터별로_합산한다() {
        // given
        List<Roster> rosters = List.of(roster(1L), roster(2L));
        given(rosterPlayerDao.findByRosterIds(Set.of(1L, 2L))).willReturn(List.of(
                rosterPlayer(1L, 100L),
                rosterPlayer(1L, 200L),
                rosterPlayer(2L, 200L),
                rosterPlayer(2L, 300L)
        ));
        given(playerDailyPointDao.findPointByPlayerIds(20260511, Set.of(100L, 200L, 300L))).willReturn(Map.of(
                100L, 10,
                200L, -5,
                300L, 20
        ));

        // when
        Map<Long, Integer> pointByRosterId = rosterPointCalculator.calculateAll(20260511, rosters);

        // then
        assertThat(pointByRosterId).containsEntry(1L, 5);
        assertThat(pointByRosterId).containsEntry(2L, 15);
        verify(rosterPlayerDao).findByRosterIds(Set.of(1L, 2L));
        verify(playerDailyPointDao).findPointByPlayerIds(20260511, Set.of(100L, 200L, 300L));
    }

    private Roster roster(long rosterId) {
        return Roster.builder()
                .rosterId(rosterId)
                .build();
    }

    private RosterPlayer rosterPlayer(long rosterId, long playerId) {
        return RosterPlayer.builder()
                .rosterId(rosterId)
                .playerId(playerId)
                .build();
    }

}
