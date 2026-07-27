package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.point.service.RosterPointCalculator;
import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.rank.model.DailyRank;
import com.kangworld.fantasybaseball.rank.model.SeasonRank;
import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.model.Roster;

@ExtendWith(MockitoExtension.class)
class RankSettlementServiceTest {

    @Mock
    private RosterDao rosterDao;

    @Mock
    private DailyRankDao dailyRankDao;

    @Mock
    private SeasonRankDao seasonRankDao;

    @Mock
    private RosterPointCalculator rosterPointCalculator;

    @Mock
    private FantasyTime fantasyTime;

    private final RankCalculator rankCalculator = new RankCalculator();

    private RankSettlementService rankSettlementService;

    /** 경기일 포인트 합계가 음수여도 일간 랭킹은 0점이고 시즌 누적 점수는 줄지 않아야 한다. */
    @Test
    void settle은_일간_음수_점수를_0점으로_보정하고_시즌_누적을_감점하지_않는다() {
        // given
        LocalDate settlementDate = LocalDate.of(2026, 5, 12);
        LocalDateTime now = LocalDateTime.of(2026, 5, 12, 2, 0);
        rankSettlementService = new RankSettlementService(
                rosterDao,
                dailyRankDao,
                seasonRankDao,
                rosterPointCalculator,
                rankCalculator,
                fantasyTime
        );
        given(fantasyTime.getDateTime()).willReturn(now);
        given(rosterDao.findByDateId(20260511)).willReturn(List.of(
                roster(1L, 10L),
                roster(2L, 20L)
        ));
        given(rosterPointCalculator.calculateAll(eq(20260512), anyList())).willReturn(Map.of(
                1L, -100,
                2L, 50
        ));
        given(dailyRankDao.findByDateId(20260511)).willReturn(List.of(
                dailyRank(20260511, 1, 10L, 60, 1),
                dailyRank(20260511, 2, 20L, 40, 2)
        ));
        given(seasonRankDao.findByDateId(20260511)).willReturn(List.of(
                seasonRank(20260511, 1, 10L, 1000, 1),
                seasonRank(20260511, 2, 30L, 900, 2)
        ));

        // when
        rankSettlementService.settle(settlementDate);

        // then
        ArgumentCaptor<List<DailyRank>> dailyRanksCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<SeasonRank>> seasonRanksCaptor = ArgumentCaptor.forClass(List.class);
        verify(dailyRankDao).insertAll(dailyRanksCaptor.capture());
        verify(seasonRankDao).insertAll(seasonRanksCaptor.capture());

        assertThat(dailyRanksCaptor.getValue()).extracting(DailyRank::getMemberId)
                .containsExactly(20L, 10L);
        assertThat(dailyRanksCaptor.getValue()).extracting(DailyRank::getPoint)
                .containsExactly(50, 0);
        assertThat(dailyRanksCaptor.getValue()).extracting(DailyRank::getPreviousRankNo)
                .containsExactly(2, 1);

        assertThat(seasonRanksCaptor.getValue()).extracting(SeasonRank::getMemberId)
                .containsExactly(10L, 30L, 20L);
        assertThat(seasonRanksCaptor.getValue()).extracting(SeasonRank::getPoint)
                .containsExactly(1000, 900, 50);
        assertThat(seasonRanksCaptor.getValue()).extracting(SeasonRank::getDailyPoint)
                .containsExactly(0, 0, 50);
        assertThat(seasonRanksCaptor.getValue()).extracting(SeasonRank::getPreviousRankNo)
                .containsExactly(1, 2, 0);
    }

    /** 같은 점수를 받은 회원은 정산일이 바뀌어도 같은 표시 순위를 받아야 한다. */
    @Test
    void settle은_동점이면_displayRankNo를_같게_계산한다() {
        // given
        LocalDate settlementDate = LocalDate.of(2026, 5, 12);
        rankSettlementService = new RankSettlementService(
                rosterDao,
                dailyRankDao,
                seasonRankDao,
                rosterPointCalculator,
                rankCalculator,
                fantasyTime
        );
        given(fantasyTime.getDateTime()).willReturn(LocalDateTime.of(2026, 5, 12, 2, 0));
        given(rosterDao.findByDateId(20260511)).willReturn(List.of(
                roster(1L, 10L),
                roster(2L, 20L),
                roster(3L, 30L)
        ));
        given(rosterPointCalculator.calculateAll(eq(20260512), anyList())).willReturn(Map.of(
                1L, 100,
                2L, 100,
                3L, 90
        ));
        given(dailyRankDao.findByDateId(20260511)).willReturn(List.of());
        given(seasonRankDao.findByDateId(20260511)).willReturn(List.of());

        // when
        rankSettlementService.settle(settlementDate);

        // then
        ArgumentCaptor<List<DailyRank>> dailyRanksCaptor = ArgumentCaptor.forClass(List.class);
        verify(dailyRankDao).insertAll(dailyRanksCaptor.capture());
        assertThat(dailyRanksCaptor.getValue()).extracting(DailyRank::getRankNo)
                .containsExactly(1, 2, 3);
        assertThat(dailyRanksCaptor.getValue()).extracting(DailyRank::getDisplayRankNo)
                .containsExactly(1, 1, 3);
        assertThat(dailyRanksCaptor.getValue()).extracting(DailyRank::getPreviousRankNo)
                .containsExactly(0, 0, 0);
    }

    private Roster roster(long rosterId, long memberId) {
        return Roster.builder()
                .rosterId(rosterId)
                .memberId(memberId)
                .dateId(20260511)
                .build();
    }

    private SeasonRank seasonRank(int dateId, int rankNo, long memberId, int point, int displayRankNo) {
        return SeasonRank.builder()
                .dateId(dateId)
                .rankNo(rankNo)
                .memberId(memberId)
                .point(point)
                .dailyPoint(0)
                .displayRankNo(displayRankNo)
                .previousRankNo(0)
                .build();
    }

    private DailyRank dailyRank(int dateId, int rankNo, long memberId, int point, int displayRankNo) {
        return DailyRank.builder()
                .dateId(dateId)
                .rankNo(rankNo)
                .memberId(memberId)
                .point(point)
                .displayRankNo(displayRankNo)
                .previousRankNo(0)
                .build();
    }

}
