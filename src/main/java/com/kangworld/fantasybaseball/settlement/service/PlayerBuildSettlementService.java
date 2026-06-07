package com.kangworld.fantasybaseball.settlement.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboPlayerProfileFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboPlayerProfile;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.Player;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerStatus;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerBuildSettlementService {

    private final KboScheduleFetchService scheduleFetchService;
    private final KboGameRecordFetchService gameRecordFetchService;
    private final KboPlayerProfileFetcher playerProfileFetcher;
    private final PlayerDao playerDao;
    private final FantasyTime fantasyTime;

    @Transactional
    public void settle(LocalDate targetDate) {
        List<KboGameRecord> gameRecords = scheduleFetchService.fetch(targetDate).stream()
                .filter(this::isSettlementTarget)
                .map(KboScheduleGame::getGameId)
                .map(gameRecordFetchService::fetch)
                .toList();

        upsertAppearedPlayers(targetDate.getYear(), gameRecords);
    }

    private boolean isSettlementTarget(KboScheduleGame game) {
        KboGameStatus status = game.getStatus();

        return status == KboGameStatus.FINISHED || status == KboGameStatus.SUSPENDED;
    }

    private void upsertAppearedPlayers(int season, List<KboGameRecord> gameRecords) {
        LocalDateTime now = fantasyTime.getDateTime();
        Map<Long, PlayerAppearance> appearanceByPlayerId = gameRecords.stream()
                .flatMap(gameRecord -> toAppearances(season, gameRecord))
                .collect(Collectors.toMap(
                        PlayerAppearance::playerId,
                        Function.identity(),
                        // 같은 선수가 더블헤더 등으로 하루에 여러 번 잡혀도 player 해석은 첫 값을 유지한다.
                        (existing, ignored) -> existing
                ));

        List<Player> players = appearanceByPlayerId.values().stream()
                .map(appearance -> toPlayer(appearance, now))
                .toList();

        playerDao.upsertAll(players);
    }

    private Stream<PlayerAppearance> toAppearances(int season, KboGameRecord gameRecord) {
        List<KboBatterRecord> batterRecords = gameRecord.getBatterRecords() == null ? List.of() : gameRecord.getBatterRecords();
        List<KboPitcherRecord> pitcherRecords = gameRecord.getPitcherRecords() == null ? List.of() : gameRecord.getPitcherRecords();

        // 한 경기 boxscore에서는 같은 선수가 같은 기록 목록에 중복 등장하지 않는 것을 기준으로 한다.
        // 예상 밖 중복이 들어오면 player 생성 목적상 첫 record를 유지한다.
        Map<Long, KboBatterRecord> batterRecordByPlayerId = batterRecords.stream()
                .collect(Collectors.toMap(
                        record -> parsePlayerId(record.getPlayerCode(), record.getName()),
                        Function.identity(),
                        (existing, ignored) -> existing
                ));
        Map<Long, KboPitcherRecord> pitcherRecordByPlayerId = pitcherRecords.stream()
                .collect(Collectors.toMap(
                        record -> parsePlayerId(record.getPlayerCode(), record.getName()),
                        Function.identity(),
                        (existing, ignored) -> existing
                ));

        // 타자/투수 한쪽에만 있는 선수와 양쪽 기록이 모두 있는 선수를 한 루프에서 해석한다.
        Set<Long> playerIds = new HashSet<>(batterRecordByPlayerId.keySet());
        playerIds.addAll(pitcherRecordByPlayerId.keySet());

        return playerIds.stream()
                .map(playerId -> toAppearance(
                        season,
                        playerId,
                        batterRecordByPlayerId.get(playerId),
                        pitcherRecordByPlayerId.get(playerId)));
    }

    private PlayerAppearance toAppearance(
            int season,
            long playerId,
            KboBatterRecord batterRecord,
            KboPitcherRecord pitcherRecord) {
        if (batterRecord != null && pitcherRecord != null)
            return resolveDualRecordAppearance(season, playerId, batterRecord.getTeamCode());

        if (batterRecord != null)
            return toBatterAppearance(playerId, batterRecord);

        return toPitcherAppearance(playerId, pitcherRecord);
    }

    private PlayerAppearance toBatterAppearance(long playerId, KboBatterRecord record) {
        PlayerPosition position = record.getPosition();

        return new PlayerAppearance(
                playerId,
                record.getName(),
                position == PlayerPosition.P ? PlayerType.PITCHER : PlayerType.BATTER,
                position,
                record.getTeamCode()
        );
    }

    private PlayerAppearance toPitcherAppearance(long playerId, KboPitcherRecord record) {
        return new PlayerAppearance(
                playerId,
                record.getName(),
                PlayerType.PITCHER,
                PlayerPosition.P,
                record.getTeamCode()
        );
    }

    private PlayerAppearance resolveDualRecordAppearance(int season, long playerId, String teamCode) {
        // KBO에는 실제 이도류가 없으므로 타자/투수 기록이 동시에 잡힌 선수는 프로필 API의 주 포지션을 우선한다.
        return playerProfileFetcher.fetch(season, playerId)
                .map(profile -> toAppearance(profile, teamCode))
                .orElseThrow(() -> {
                    log.warn("KBO dual record player profile is missing. season={}, playerId={}", season, playerId);

                    return new IllegalStateException("투타 기록이 모두 존재하는 선수의 프로필을 조회하지 못했습니다. playerId=" + playerId);
                });
    }

    private PlayerAppearance toAppearance(KboPlayerProfile profile, String teamCode) {
        return new PlayerAppearance(
                profile.getPlayerId(),
                profile.getName(),
                profile.getType(),
                profile.getPosition(),
                teamCode
        );
    }

    private Player toPlayer(PlayerAppearance appearance, LocalDateTime now) {
        return Player.builder()
                .id(appearance.playerId())
                .name(appearance.name())
                .type(appearance.type())
                .position(appearance.position())
                .teamCode(appearance.teamCode())
                .status(PlayerStatus.ACTIVE)
                .teamUpdatedAt(now)
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

    private record PlayerAppearance(
            long playerId,
            String name,
            PlayerType type,
            PlayerPosition position,
            String teamCode) {
    }

}
