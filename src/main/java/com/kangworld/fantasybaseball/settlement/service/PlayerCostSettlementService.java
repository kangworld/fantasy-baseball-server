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
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerCostSettlementService {

    private static final int DEFAULT_NEW_PLAYER_COST = 3;
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

    public void settle(LocalDate targetDate) {
        if (!COST_BUILD_DAYS.contains(targetDate.getDayOfMonth()))
            return;

        List<PlayerProfile> profiles = playerProfilePointDao.findByDateId(DateIds.from(targetDate));
        if (profiles.isEmpty())
            return;

        // 코스트 산정 기준은 정산 대상일을 포함한 직전 15일 선수별 합산 포인트다.
        Set<Long> playerIds = profiles.stream()
                .map(PlayerProfile::getPlayerId)
                .collect(Collectors.toSet());
        Map<Long, Integer> pointByPlayerId = playerDailyPointDao.findTotalPointByPlayerIdsBetweenDateIds(
                DateIds.from(targetDate.minusDays(BIWEEK_DAYS)),
                DateIds.from(targetDate),
                playerIds);
        List<PlayerProfile> updatedProfiles = buildCostProfiles(profiles, pointByPlayerId);

        playerProfilePointDao.upsertAll(updatedProfiles);
    }

    private List<PlayerProfile> buildCostProfiles(List<PlayerProfile> profiles, Map<Long, Integer> pointByPlayerId) {
        // 집계 구간에 포인트가 없는 선수는 percentile 계산에서 제외하고 신규 선수 기본 cost를 유지한다.
        Map<Boolean, List<PlayerProfile>> profilesPartitionedByPitcher = profiles.stream()
                .filter(profile -> pointByPlayerId.containsKey(profile.getPlayerId()))
                .collect(Collectors.partitioningBy(this::isPitcher));
        List<PlayerProfile> costedPitchers = buildCostProfilesByGroup(profilesPartitionedByPitcher.get(true), pointByPlayerId);
        List<PlayerProfile> costedBatters = buildCostProfilesByGroup(profilesPartitionedByPitcher.get(false), pointByPlayerId);
        Map<Long, PlayerProfile> costedProfileByPlayerId = Stream.concat(costedPitchers.stream(), costedBatters.stream())
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));

        return profiles.stream()
                .map(profile -> {
                    PlayerProfile costedProfile = costedProfileByPlayerId.get(profile.getPlayerId());
                    if (costedProfile != null)
                        return costedProfile;

                    return withCost(profile, DEFAULT_NEW_PLAYER_COST);
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
                .teamCode(profile.getTeamCode())
                .pos(profile.getPos())
                .cost(cost)
                .point(profile.getPoint())
                .avgPoint(profile.getAvgPoint())
                .pointGameCount(profile.getPointGameCount())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private boolean isPitcher(PlayerProfile profile) {
        return "P".equals(profile.getPos());
    }

    private record CostPercent(int cost, double percent) {
    }

    private record PlayerPoint(long playerId, int point) {
    }

}
