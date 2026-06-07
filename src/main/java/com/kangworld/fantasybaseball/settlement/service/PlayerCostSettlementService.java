package com.kangworld.fantasybaseball.settlement.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.player.model.PlayerSummary;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerCostSettlementService {

    private static final int NO_RECENT_RECORD_COST = 1;
    private static final int BIWEEK_DAYS = 14;
    private static final Set<Integer> COST_BUILD_DAYS = Set.of(1, 16);
    private static final List<CostPercent> COST_PERCENTS = List.of(
            new CostPercent(5, 0.03),
            new CostPercent(4, 0.08),
            new CostPercent(3, 0.15),
            new CostPercent(2, 0.25),
            new CostPercent(1, 1.0)
    );

    private final PlayerDailyPointDao playerDailyPointDao;
    private final PlayerProfilePointDao playerProfilePointDao;
    private final PlayerDao playerDao;

    public void settle(LocalDate targetDate) {
        if (!COST_BUILD_DAYS.contains(targetDate.getDayOfMonth()))
            return;

        List<PlayerProfile> profiles = playerProfilePointDao.findByDateId(DateIds.from(targetDate));
        if (profiles.isEmpty())
            return;

        // 코스트 산정 기준은 정산 대상일을 포함한 직전 15일 선수별 합산 포인트다.
        Set<Long> playerIds = profiles.stream().map(PlayerProfile::getPlayerId).collect(Collectors.toSet());
        Map<Long, Integer> pointByPlayerId = playerDailyPointDao.findTotalPointByPlayerIdsBetweenDateIds(
                DateIds.from(targetDate.minusDays(BIWEEK_DAYS)), DateIds.from(targetDate), playerIds);

        List<PlayerProfile> updatedProfiles = buildCostProfiles(DateIds.from(targetDate), profiles, pointByPlayerId);

        playerProfilePointDao.updateCostAll(updatedProfiles);
    }

    private List<PlayerProfile> buildCostProfiles(
            int dateId,
            List<PlayerProfile> profiles,
            Map<Long, Integer> pointByPlayerId) {
        // 집계 구간에 포인트가 없는 선수는 percentile 계산에서 제외하고 최저 cost로 내린다.
        Set<Long> playerIds = profiles.stream().map(PlayerProfile::getPlayerId).collect(Collectors.toSet());

        Map<Long, PlayerSummary> summaryByPlayerId = playerDao.findSummariesByPlayerIds(dateId, playerIds).stream()
                .collect(Collectors.toMap(PlayerSummary::getId, Function.identity()));
        // player_profile에는 포지션을 저장하지 않으므로 cost 그룹 분리는 player master의 타입을 따른다.
        Set<Long> pitcherPlayerIds = summaryByPlayerId.values().stream()
                .filter(summary -> summary.getType() == PlayerType.PITCHER)
                .map(PlayerSummary::getId)
                .collect(Collectors.toSet());

        Map<Boolean, List<PlayerProfile>> profilesPartitionedByPitcher = profiles.stream()
                .filter(profile -> pointByPlayerId.containsKey(profile.getPlayerId()))
                .collect(Collectors.partitioningBy(profile -> pitcherPlayerIds.contains(profile.getPlayerId())));

        List<PlayerProfile> costedPitchers = buildCostProfilesByGroup(profilesPartitionedByPitcher.get(true), pointByPlayerId);
        List<PlayerProfile> costedBatters = buildCostProfilesByGroup(profilesPartitionedByPitcher.get(false), pointByPlayerId);
        Map<Long, PlayerProfile> costedProfileByPlayerId = Stream.concat(costedPitchers.stream(), costedBatters.stream())
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));

        return profiles.stream()
                .map(profile -> {
                    PlayerProfile costedProfile = costedProfileByPlayerId.get(profile.getPlayerId());
                    if (costedProfile != null)
                        return costedProfile;

                    return withCost(profile, NO_RECENT_RECORD_COST);
                }).toList();
    }

    private List<PlayerProfile> buildCostProfilesByGroup(
            List<PlayerProfile> profiles,
            Map<Long, Integer> pointByPlayerId) {
        List<PlayerPoint> playerPoints = profiles.stream()
                .map(profile -> new PlayerPoint(profile.getPlayerId(), pointByPlayerId.get(profile.getPlayerId())))
                .sorted(Comparator.comparing(PlayerPoint::point).reversed())
                .toList();
        AtomicInteger start = new AtomicInteger();

        Map<Long, PlayerProfile> profileByPlayerId = profiles.stream()
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));

        // 투수와 타자는 각 그룹 내부 순위로 cost를 부여한다.
        return COST_PERCENTS.stream()
                .flatMap(costPercent -> {
                    int from = start.get();
                    int to = Math.min(from + (int) (costPercent.percent() * profiles.size()), playerPoints.size());
                    start.set(to);

                    return IntStream.range(from, to)
                            .mapToObj(index -> withCost(profileByPlayerId.get(playerPoints.get(index).playerId()), costPercent.cost()));
                })
                .toList();
    }

    private PlayerProfile withCost(PlayerProfile profile, int cost) {
        return PlayerProfile.builder()
                .dateId(profile.getDateId())
                .playerId(profile.getPlayerId())
                .cost(cost)
                .point(profile.getPoint())
                .avgPoint(profile.getAvgPoint())
                .pointGameCount(profile.getPointGameCount())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private record CostPercent(int cost, double percent) {
    }

    private record PlayerPoint(long playerId, int point) {
    }

}
