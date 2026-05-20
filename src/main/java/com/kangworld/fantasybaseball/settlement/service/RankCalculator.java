package com.kangworld.fantasybaseball.settlement.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

@Component
public class RankCalculator {

    public List<RankedMemberPoint> calculate(List<MemberPoint> points) {
        List<MemberPoint> sortedPoints = points.stream()
                .sorted(Comparator.comparingInt(MemberPoint::point).reversed()
                        .thenComparingLong(MemberPoint::memberId))
                .toList();

        RankingState rankingState = new RankingState();

        // rankNo는 행 번호, displayRankNo는 동점자를 같은 순위로 보여주는 화면용 순위다.
        return IntStream.range(0, sortedPoints.size())
                .mapToObj(index -> rankingState.rank(index, sortedPoints.get(index)))
                .toList();
    }

    public record MemberPoint(long memberId, int point) {
    }

    public record RankedMemberPoint(int rankNo, long memberId, int point, int displayRankNo) {
    }

    private static class RankingState {

        private int previousPoint = Integer.MIN_VALUE;
        private int displayRankNo = 0;

        private RankedMemberPoint rank(int index, MemberPoint point) {
            int rankNo = index + 1;

            if (point.point() != previousPoint) {
                displayRankNo = rankNo;
                previousPoint = point.point();
            }

            return new RankedMemberPoint(rankNo, point.memberId(), point.point(), displayRankNo);
        }

    }

}
