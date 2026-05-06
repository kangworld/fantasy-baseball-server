package com.kangworld.fantasybaseball.settlement.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.point.service.RosterPointCalculator;
import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.rank.model.DailyRank;
import com.kangworld.fantasybaseball.rank.model.SeasonRank;
import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.settlement.service.RankCalculator.MemberPoint;
import com.kangworld.fantasybaseball.settlement.service.RankCalculator.RankedMemberPoint;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankSettlementService {

    private final RosterDao rosterDao;
    private final DailyRankDao dailyRankDao;
    private final SeasonRankDao seasonRankDao;
    private final RosterPointCalculator rosterPointCalculator;
    private final RankCalculator rankCalculator;
    private final FantasyTime fantasyTime;

    @Transactional
    public void settle(LocalDate targetDate) {
        int dateId = DateIds.from(targetDate);
        LocalDateTime now = fantasyTime.getDateTime();

        List<DailyRank> dailyRanks = createDailyRanks(targetDate, now);
        dailyRankDao.insertAll(dailyRanks);

        List<SeasonRank> seasonRanks = createSeasonRanks(targetDate, dailyRanks, now);
        seasonRankDao.insertAll(seasonRanks);
    }

    private List<DailyRank> createDailyRanks(LocalDate targetDate, LocalDateTime now) {
        int dateId = DateIds.from(targetDate);
        int previousDateId = DateIds.from(targetDate.minusDays(1));
        Map<Long, DailyRank> previousRankByMemberId = toDailyRankMap(dailyRankDao.findByDateId(previousDateId));
        List<Roster> rosters = rosterDao.findByDateId(dateId);
        Map<Long, Integer> pointByRosterId = rosterPointCalculator.calculateAll(dateId, rosters);
        List<MemberPoint> memberPoints = rosters.stream()
                .map(roster -> new MemberPoint(roster.getMemberId(), calculateDailyPoint(roster, pointByRosterId)))
                .toList();

        return rankCalculator.calculate(memberPoints).stream()
                .map(rank -> toDailyRank(dateId, rank, previousDisplayRankNo(rank, previousRankByMemberId), now))
                .toList();
    }

    private int calculateDailyPoint(Roster roster, Map<Long, Integer> pointByRosterId) {
        int rawPoint = pointByRosterId.getOrDefault(roster.getRosterId(), 0);
        // 일간 원점수가 음수여도 회원 랭킹 포인트는 감점하지 않는다.
        return Math.max(rawPoint, 0);
    }

    private List<SeasonRank> createSeasonRanks(LocalDate targetDate, List<DailyRank> dailyRanks, LocalDateTime now) {
        int dateId = DateIds.from(targetDate);
        int previousDateId = DateIds.from(targetDate.minusDays(1));
        Map<Long, SeasonRank> previousRankByMemberId = toPreviousRankMap(seasonRankDao.findByDateId(previousDateId));
        Map<Long, DailyRank> dailyRankByMemberId = toDailyRankMap(dailyRanks);

        // 미제출 회원도 시즌 랭킹에는 계속 보여야 하므로 전일 시즌 랭킹과 당일 제출자를 합친다.
        Map<Long, Integer> seasonPointByMemberId = new HashMap<>();
        previousRankByMemberId.forEach((memberId, rank) -> seasonPointByMemberId.put(memberId, rank.getPoint()));
        dailyRankByMemberId.forEach((memberId, rank) ->
                seasonPointByMemberId.merge(memberId, rank.getPoint(), Integer::sum)
        );

        List<MemberPoint> memberPoints = seasonPointByMemberId.entrySet().stream()
                .map(entry -> new MemberPoint(entry.getKey(), entry.getValue()))
                .toList();

        return rankCalculator.calculate(memberPoints).stream()
                .map(rank -> toSeasonRank(dateId, rank, dailyRankByMemberId, previousRankByMemberId, now))
                .toList();
    }

    private Map<Long, SeasonRank> toPreviousRankMap(List<SeasonRank> ranks) {
        Map<Long, SeasonRank> rankByMemberId = new HashMap<>();
        ranks.forEach(rank -> rankByMemberId.put(rank.getMemberId(), rank));

        return rankByMemberId;
    }

    private Map<Long, DailyRank> toDailyRankMap(List<DailyRank> ranks) {
        Map<Long, DailyRank> rankByMemberId = new HashMap<>();
        ranks.forEach(rank -> rankByMemberId.put(rank.getMemberId(), rank));

        return rankByMemberId;
    }

    private DailyRank toDailyRank(
            int dateId,
            RankedMemberPoint rank,
            int previousRankNo,
            LocalDateTime now) {
        return DailyRank.builder()
                .dateId(dateId)
                .rankNo(rank.rankNo())
                .memberId(rank.memberId())
                .point(rank.point())
                .displayRankNo(rank.displayRankNo())
                .previousRankNo(previousRankNo)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private int previousDisplayRankNo(RankedMemberPoint rank, Map<Long, DailyRank> previousRankByMemberId) {
        DailyRank previousRank = previousRankByMemberId.get(rank.memberId());

        return previousRank == null ? 0 : previousRank.getDisplayRankNo();
    }

    private SeasonRank toSeasonRank(
            int dateId,
            RankedMemberPoint rank,
            Map<Long, DailyRank> dailyRankByMemberId,
            Map<Long, SeasonRank> previousRankByMemberId,
            LocalDateTime now) {
        DailyRank dailyRank = dailyRankByMemberId.get(rank.memberId());
        SeasonRank previousRank = previousRankByMemberId.get(rank.memberId());

        return SeasonRank.builder()
                .dateId(dateId)
                .rankNo(rank.rankNo())
                .memberId(rank.memberId())
                .point(rank.point())
                .dailyPoint(dailyRank == null ? 0 : dailyRank.getPoint())
                .displayRankNo(rank.displayRankNo())
                .previousRankNo(previousRank == null ? 0 : previousRank.getDisplayRankNo())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
