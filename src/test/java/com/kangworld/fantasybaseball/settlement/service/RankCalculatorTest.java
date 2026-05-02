package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kangworld.fantasybaseball.settlement.service.RankCalculator.MemberPoint;
import com.kangworld.fantasybaseball.settlement.service.RankCalculator.RankedMemberPoint;

class RankCalculatorTest {

    private final RankCalculator rankCalculator = new RankCalculator();

    @Test
    void calculate는_점수_내림차순과_회원_id_오름차순으로_rank와_displayRank를_계산한다() {
        // given
        List<MemberPoint> points = List.of(
                new MemberPoint(30L, 90),
                new MemberPoint(20L, 100),
                new MemberPoint(10L, 100)
        );

        // when
        List<RankedMemberPoint> ranks = rankCalculator.calculate(points);

        // then
        assertThat(ranks).extracting(RankedMemberPoint::memberId)
                .containsExactly(10L, 20L, 30L);
        assertThat(ranks).extracting(RankedMemberPoint::rankNo)
                .containsExactly(1, 2, 3);
        assertThat(ranks).extracting(RankedMemberPoint::displayRankNo)
                .containsExactly(1, 1, 3);
    }

}
