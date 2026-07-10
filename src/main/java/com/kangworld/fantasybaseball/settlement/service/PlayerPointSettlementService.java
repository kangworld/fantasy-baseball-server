package com.kangworld.fantasybaseball.settlement.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;
import com.kangworld.fantasybaseball.point.dao.PointRuleDao;
import com.kangworld.fantasybaseball.point.model.CalculatedPlayerPoint;
import com.kangworld.fantasybaseball.point.model.PlayerDailyPoint;
import com.kangworld.fantasybaseball.point.model.PointRule;
import com.kangworld.fantasybaseball.point.service.PlayerGameRecordMapper;
import com.kangworld.fantasybaseball.point.service.PointCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerPointSettlementService {

    private static final int NEW_PLAYER_COST = 2;

    private final KboScheduleFetchService scheduleFetchService;
    private final KboGameRecordFetchService gameRecordFetchService;
    private final PointRuleDao pointRuleDao;
    private final PlayerDailyPointDao playerDailyPointDao;
    private final PlayerProfilePointDao playerProfilePointDao;
    private final PlayerGameRecordMapper playerGameRecordMapper;
    private final PointCalculator pointCalculator;
    private final FantasyTime fantasyTime;

    @Transactional
    public void settle(LocalDate settlementDate, LocalDate gameDate) {
        // 정산은 완료 또는 서스펜디드 경기만 확정 데이터로 보고, 진행 중 경기는 실시간 조회에만 사용한다.
        List<KboGameRecord> gameRecords = scheduleFetchService.fetch(gameDate).stream()
                .filter(this::isSettlementTarget)
                .map(KboScheduleGame::getGameId)
                .map(gameRecordFetchService::fetch)
                .toList();

        applyGameRecords(settlementDate, gameRecords);
    }

    private boolean isSettlementTarget(KboScheduleGame game) {
        KboGameStatus status = game.getStatus();

        return status == KboGameStatus.FINISHED || status == KboGameStatus.SUSPENDED;
    }

    private void applyGameRecords(LocalDate targetDate, List<KboGameRecord> gameRecords) {
        // 하루치 record API 결과를 선수 일간 포인트와 프로필 누적값으로 반영한다.
        int dateId = DateIds.from(targetDate);
        LocalDateTime now = fantasyTime.getDateTime();

        List<PlayerDailyPoint> dailyPoints = calculateDailyPoints(dateId, gameRecords, now);
        playerDailyPointDao.upsertAll(dailyPoints);

        updateProfiles(targetDate, gameRecords, dailyPoints, now);
    }

    private List<PlayerDailyPoint> calculateDailyPoints(
            int dateId,
            List<KboGameRecord> gameRecords,
            LocalDateTime now) {
        // 배점 룰은 한 번만 조회한다.
        Map<PlayerType, List<PointRule>> rulesByType = pointRuleDao.findAllActive().stream()
                .collect(Collectors.groupingBy(PointRule::getPlayerType));
        List<PointRule> batterRules = rulesByType.getOrDefault(PlayerType.BATTER, List.of());
        List<PointRule> pitcherRules = rulesByType.getOrDefault(PlayerType.PITCHER, List.of());

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
        // 같은 선수의 타자/투수 점수는 이후 playerId 기준으로 합산한다.
        Stream<CalculatedPlayerPoint> batterPoints = playerGameRecordMapper.toBatterRecords(gameRecord).stream()
                .map(record -> pointCalculator.calculateBatter(record, batterRules));

        Stream<CalculatedPlayerPoint> pitcherPoints = playerGameRecordMapper.toPitcherRecords(gameRecord).stream()
                .map(record -> pointCalculator.calculatePitcher(record, pitcherRules));

        return Stream.concat(batterPoints, pitcherPoints);
    }

    private void updateProfiles(
            LocalDate targetDate,
            List<KboGameRecord> gameRecords,
            List<PlayerDailyPoint> dailyPoints,
            LocalDateTime now) {
        // 전날 선수풀을 유지하되, 당일 기록에 처음 등장한 선수도 신규 profile로 편입한다.
        int dateId = DateIds.from(targetDate);
        int previousDateId = DateIds.from(targetDate.minusDays(1));

        // 같은 날 여러 점수 row가 있는 선수는 선수 단위로 합산한다.
        Map<Long, Integer> dailyPointByPlayerId = dailyPoints.stream()
                .collect(Collectors.groupingBy(
                        PlayerDailyPoint::getPlayerId,
                        Collectors.summingInt(PlayerDailyPoint::getPoint)
                ));
        Map<Long, PlayerProfile> previousProfileByPlayerId = playerProfilePointDao.findByDateId(previousDateId)
                .stream()
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));
        Set<Long> appearedPlayerIds = getAppearedPlayerIds(gameRecords);

        Set<Long> profileTargetPlayerIds = new HashSet<>(previousProfileByPlayerId.keySet());
        profileTargetPlayerIds.addAll(appearedPlayerIds);

        List<PlayerProfile> profiles = profileTargetPlayerIds.stream()
                .sorted(Comparator.naturalOrder())
                .map(playerId -> toProfile(
                        dateId,
                        playerId,
                        previousProfileByPlayerId.get(playerId),
                        appearedPlayerIds.contains(playerId),
                        dailyPointByPlayerId.getOrDefault(playerId, 0),
                        now))
                .toList();

        playerProfilePointDao.insertAll(profiles);
    }

    private Set<Long> getAppearedPlayerIds(List<KboGameRecord> gameRecords) {
        return gameRecords.stream()
                .flatMap(this::toAppearedPlayerIds)
                .collect(Collectors.toSet());
    }

    private Stream<Long> toAppearedPlayerIds(KboGameRecord gameRecord) {
        List<KboBatterRecord> batterRecords =
                gameRecord.getBatterRecords() == null ? List.of() : gameRecord.getBatterRecords();
        List<KboPitcherRecord> pitcherRecords =
                gameRecord.getPitcherRecords() == null ? List.of() : gameRecord.getPitcherRecords();

        Stream<Long> batters = batterRecords.stream()
                .map(record -> parsePlayerId(record.getPlayerCode(), record.getName()));
        Stream<Long> pitchers = pitcherRecords.stream()
                .map(record -> parsePlayerId(record.getPlayerCode(), record.getName()));

        return Stream.concat(batters, pitchers);
    }

    private PlayerProfile toProfile(
            int dateId,
            long playerId,
            PlayerProfile previous,
            boolean appeared,
            int dailyPoint,
            LocalDateTime now) {
        int previousPoint = previous == null ? 0 : previous.getPoint();
        int cumulativePoint = previousPoint + dailyPoint;
        // 실제 계산 대상 등장 횟수를 평균 분모로 사용한다.
        int previousPointGameCount = previous == null ? 0 : previous.getPointGameCount();
        int pointGameCount = previousPointGameCount + (appeared ? 1 : 0);

        return PlayerProfile.builder()
                .dateId(dateId)
                .playerId(playerId)
                .cost(previous == null ? NEW_PLAYER_COST : previous.getCost())
                .point(cumulativePoint)
                .avgPoint(pointGameCount == 0 ? 0 : cumulativePoint / pointGameCount)
                .pointGameCount(pointGameCount)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private long parsePlayerId(String playerCode, String playerName) {
        if (playerCode == null || playerCode.isBlank()) {
            log.warn("KBO record player code is empty. playerName={}", playerName);
            throw new IllegalStateException("KBO 선수 ID가 비어 있습니다.");
        }

        try {
            return Long.parseLong(playerCode);
        } catch (NumberFormatException exception) {
            log.warn("KBO record player code is invalid. playerCode={}, playerName={}", playerCode, playerName, exception);
            throw new IllegalStateException("KBO 선수 ID 형식이 올바르지 않습니다.", exception);
        }
    }

    private record DailyPointKey(String gameId, long playerId) {
    }

}
