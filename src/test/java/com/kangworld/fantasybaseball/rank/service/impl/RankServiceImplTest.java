package com.kangworld.fantasybaseball.rank.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.rank.dao.view.DailyRankView;
import com.kangworld.fantasybaseball.rank.dao.view.SeasonRankView;
import com.kangworld.fantasybaseball.rank.service.dto.DailyRankPageResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardResponse;
import com.kangworld.fantasybaseball.rank.service.dto.SeasonRankPageResponse;

@ExtendWith(MockitoExtension.class)
class RankServiceImplTest {

    @Mock
    private DailyRankDao dailyRankDao;

    @Mock
    private SeasonRankDao seasonRankDao;

    @Mock
    private FantasyTime fantasyTime;

    private RankServiceImpl rankService;

    @BeforeEach
    void setUp() {
        rankService = new RankServiceImpl(dailyRankDao, seasonRankDao, fantasyTime);
    }

    @Test
    void findMyScoreboard는_date가_없으면_FantasyTime_now를_사용한다() {
        // given
        given(fantasyTime.now()).willReturn(LocalDate.of(2026, 5, 11));
        given(dailyRankDao.findViewByMemberIdAndDateId(1L, 20260511))
                .willReturn(Optional.of(dailyRank(3, 2, 1L, "강민구", 120)));
        given(seasonRankDao.findViewByMemberIdAndDateId(1L, 20260511))
                .willReturn(Optional.of(seasonRank(5, 4, 7, 1L, "강민구", 1020, 120)));

        // when
        ScoreboardResponse response = rankService.findMyScoreboard(1L, null);

        // then
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(response.getDaily().getPoint()).isEqualTo(120);
        assertThat(response.getDaily().getDisplayRankNo()).isEqualTo(2);
        assertThat(response.getSeason().getPoint()).isEqualTo(1020);
        assertThat(response.getSeason().getPreviousRankNo()).isEqualTo(7);
    }

    @Test
    void findMyScoreboard는_랭킹이_없으면_0점_객체를_반환하고_캐시하지_않는다() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 11);
        given(dailyRankDao.findViewByMemberIdAndDateId(1L, 20260511)).willReturn(Optional.empty());
        given(seasonRankDao.findViewByMemberIdAndDateId(1L, 20260511)).willReturn(Optional.empty());

        // when
        ScoreboardResponse first = rankService.findMyScoreboard(1L, date);
        ScoreboardResponse second = rankService.findMyScoreboard(1L, date);

        // then
        assertThat(first.getDaily().getPoint()).isZero();
        assertThat(first.getDaily().getRankNo()).isZero();
        assertThat(first.getSeason().getPoint()).isZero();
        assertThat(second.getSeason().getDisplayRankNo()).isZero();
        verify(dailyRankDao, times(2)).findViewByMemberIdAndDateId(1L, 20260511);
        verify(seasonRankDao, times(2)).findViewByMemberIdAndDateId(1L, 20260511);
    }

    @Test
    void findDailyRanks는_페이지를_캐시한다() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 11);
        given(dailyRankDao.findViewsByDateId(20260511, 50, 0))
                .willReturn(List.of(dailyRank(1, 1, 1L, "강민구", 100)));

        // when
        DailyRankPageResponse first = rankService.findDailyRanks(date, 0, 50);
        DailyRankPageResponse second = rankService.findDailyRanks(date, 0, 50);

        // then
        assertThat(first.getRanks()).hasSize(1);
        assertThat(second.getRanks()).hasSize(1);
        verify(dailyRankDao, times(1)).findViewsByDateId(20260511, 50, 0);
    }

    @Test
    void findSeasonRanks는_page와_size로_limit_offset을_계산한다() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 11);
        given(seasonRankDao.findViewsByDateId(20260511, 20, 20))
                .willReturn(List.of(seasonRank(21, 21, 10, 1L, "강민구", 1000, 20)));

        // when
        SeasonRankPageResponse response = rankService.findSeasonRanks(date, 1, 20);

        // then
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getRanks()).hasSize(1);
        assertThat(response.getRanks().get(0).getRankNo()).isEqualTo(21);
        verify(seasonRankDao).findViewsByDateId(20260511, 20, 20);
        verify(fantasyTime, never()).now();
    }

    private DailyRankView dailyRank(int rankNo, int displayRankNo, long memberId, String displayName, int point) {
        return DailyRankView.builder()
                .rankNo(rankNo)
                .displayRankNo(displayRankNo)
                .memberId(memberId)
                .displayName(displayName)
                .point(point)
                .build();
    }

    private SeasonRankView seasonRank(
            int rankNo,
            int displayRankNo,
            int previousRankNo,
            long memberId,
            String displayName,
            int point,
            int dailyPoint) {
        return SeasonRankView.builder()
                .rankNo(rankNo)
                .displayRankNo(displayRankNo)
                .previousRankNo(previousRankNo)
                .memberId(memberId)
                .displayName(displayName)
                .point(point)
                .dailyPoint(dailyPoint)
                .build();
    }

}
