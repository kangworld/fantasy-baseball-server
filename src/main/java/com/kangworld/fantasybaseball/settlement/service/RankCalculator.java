package com.kangworld.fantasybaseball.settlement.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class RankCalculator {

    public List<RankedMemberPoint> calculate(List<MemberPoint> points) {
        List<MemberPoint> sortedPoints = points.stream()
                .sorted(Comparator.comparingInt(MemberPoint::point).reversed()
                        .thenComparingLong(MemberPoint::memberId))
                .toList();

        List<RankedMemberPoint> ranks = new ArrayList<>();
        int previousPoint = Integer.MIN_VALUE;
        int displayRankNo = 0;

        // rankNo는 행 번호, displayRankNo는 동점자를 같은 순위로 보여주는 화면용 순위다.
        for (int index = 0; index < sortedPoints.size(); index++) {
            MemberPoint point = sortedPoints.get(index);
            int rankNo = index + 1;

            if (point.point() != previousPoint) {
                displayRankNo = rankNo;
                previousPoint = point.point();
            }

            ranks.add(new RankedMemberPoint(rankNo, point.memberId(), point.point(), displayRankNo));
        }

        return List.copyOf(ranks);
    }

    public record MemberPoint(long memberId, int point) {
    }

    public record RankedMemberPoint(int rankNo, long memberId, int point, int displayRankNo) {
    }

}
