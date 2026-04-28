package com.kangworld.fantasybaseball.point.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.point.dao.PointRuleDao;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;
import com.kangworld.fantasybaseball.point.model.CalculatedPlayerPoint;
import com.kangworld.fantasybaseball.point.model.PointRule;
import com.kangworld.fantasybaseball.point.model.PlayerDailyPoint;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerProfilePointBuildService {

    private final PointRuleDao pointRuleDao;
    private final PlayerDailyPointDao playerDailyPointDao;
    private final PlayerProfilePointDao playerProfilePointDao;
    private final PlayerGameRecordMapper playerGameRecordMapper;
    private final PointCalculator pointCalculator;
    private final FantasyTime fantasyTime;

    @Transactional
    public void build(LocalDate targetDate, List<KboGameRecord> gameRecords) {
        // 하루치 record API 결과를 선수 일간 포인트와 프로필 누적값으로 반영
        int dateId = DateIds.from(targetDate);
        LocalDateTime now = fantasyTime.getDateTime();

        List<PlayerDailyPoint> dailyPoints = calculateDailyPoints(dateId, gameRecords, now);
        playerDailyPointDao.upsertAll(dailyPoints);

        updateProfiles(targetDate, dailyPoints, now);
    }

    private List<PlayerDailyPoint> calculateDailyPoints(
            int dateId,
            List<KboGameRecord> gameRecords,
            LocalDateTime now) {
        // 배점 룰은 한 번만 조회
        Map<PlayerType, List<PointRule>> rulesByType = pointRuleDao.findAllActive().stream()
                .collect(Collectors.groupingBy(PointRule::getPlayerType));
        List<PointRule> batterRules = rulesByType.getOrDefault(PlayerType.BATTER, Collections.emptyList());
        List<PointRule> pitcherRules = rulesByType.getOrDefault(PlayerType.PITCHER, Collections.emptyList());

        Map<DailyPointKey, Integer> pointByKey = gameRecords.stream()
                .flatMap(gameRecord -> calculateGamePoints(gameRecord, batterRules, pitcherRules))
                .collect(Collectors.groupingBy(
                        point -> new DailyPointKey(point.gameId(), point.playerId()),
                        Collectors.summingInt(CalculatedPlayerPoint::point)
                ));

        return pointByKey.entrySet().stream()
                .map(entry -> {
                    DailyPointKey key = entry.getKey();

                    return PlayerDailyPoint.builder()
                            .dateId(dateId)
                            .gameId(key.gameId())
                            .playerId(key.playerId())
                            .point(entry.getValue())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                })
                .toList();
    }

    private Stream<CalculatedPlayerPoint> calculateGamePoints(
            KboGameRecord gameRecord,
            List<PointRule> batterRules,
            List<PointRule> pitcherRules) {
        // 같은 선수의 타자/투수 점수는 이후 playerId 기준으로 합산
        Stream<CalculatedPlayerPoint> batterPoints = playerGameRecordMapper.toBatterRecords(gameRecord).stream()
                .map(record -> pointCalculator.calculateBatter(record, batterRules));

        Stream<CalculatedPlayerPoint> pitcherPoints = playerGameRecordMapper.toPitcherRecords(gameRecord).stream()
                .map(record -> pointCalculator.calculatePitcher(record, pitcherRules));

        return Stream.concat(batterPoints, pitcherPoints);
    }

    private void updateProfiles(
            LocalDate targetDate,
            List<PlayerDailyPoint> dailyPoints,
            LocalDateTime now) {
        // 전날 프로필 이월 후 오늘 계산된 선수만 누적값 증가
        int dateId = DateIds.from(targetDate);
        int previousDateId = DateIds.from(targetDate.minusDays(1));

        // 같은 날 여러 점수 row가 있는 선수는 선수 단위 합산
        Map<Long, Integer> dailyPointByPlayerId = dailyPoints.stream()
                .collect(Collectors.groupingBy(
                        PlayerDailyPoint::getPlayerId,
                        Collectors.summingInt(PlayerDailyPoint::getPoint)
                ));
        Set<Long> appearedPlayerIds = dailyPoints.stream()
                .map(PlayerDailyPoint::getPlayerId)
                .collect(Collectors.toSet());

        List<PlayerProfile> profiles = playerProfilePointDao.findByDateId(previousDateId).stream()
                .map(previous -> toProfile(dateId, previous, dailyPointByPlayerId.getOrDefault(previous.getPlayerId(), 0),
                        appearedPlayerIds.contains(previous.getPlayerId()), now))
                .toList();

        playerProfilePointDao.upsertAll(profiles);
    }

    private PlayerProfile toProfile(
            int dateId,
            PlayerProfile previous,
            int dailyPoint,
            boolean appeared,
            LocalDateTime now) {
        int cumulativePoint = previous.getPoint() + dailyPoint;
        // 실제 계산 대상 등장 횟수를 평균 분모로 사용
        int pointGameCount = previous.getPointGameCount() + (appeared ? 1 : 0);

        return PlayerProfile.builder()
                .dateId(dateId)
                .playerId(previous.getPlayerId())
                .teamCode(previous.getTeamCode())
                .pos(previous.getPos())
                .cost(previous.getCost())
                .point(cumulativePoint)
                .avgPoint(cumulativePoint / Math.max(1, pointGameCount))
                .pointGameCount(pointGameCount)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private record DailyPointKey(String gameId, long playerId) {
    }

}
