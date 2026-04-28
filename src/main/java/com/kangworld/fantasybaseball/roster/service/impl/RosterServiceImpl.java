package com.kangworld.fantasybaseball.roster.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
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
import com.kangworld.fantasybaseball.game.dao.GameDao;
import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerSummary;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.exception.RosterAlreadyExistsException;
import com.kangworld.fantasybaseball.roster.exception.RosterNotFoundException;
import com.kangworld.fantasybaseball.roster.exception.RosterValidationException;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;
import com.kangworld.fantasybaseball.roster.service.RosterService;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPlayerRequest;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPlayerResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterSubmitRequest;
import com.kangworld.rediscore.lock.annotation.RedisLock;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RosterServiceImpl implements RosterService {

    private static final int ROSTER_PLAYER_COUNT = 14;
    private static final int BATTER_COUNT = 9;
    private static final int PITCHER_COUNT = 5;
    private static final String STARTED_GAME_PLAYER_CHANGE_MESSAGE = "이미 시작한 경기의 선수는 변경할 수 없습니다.";
    private static final Set<PlayerPosition> REQUIRED_BATTER_POSITIONS = EnumSet.of(
            PlayerPosition.C,
            PlayerPosition.B1,
            PlayerPosition.B2,
            PlayerPosition.B3,
            PlayerPosition.SS,
            PlayerPosition.LF,
            PlayerPosition.CF,
            PlayerPosition.RF,
            PlayerPosition.DH
    );

    private final RosterDao rosterDao;
    private final RosterPlayerDao rosterPlayerDao;
    private final PlayerDao playerDao;
    private final GameDao gameDao;
    private final FantasyTime fantasyTime;

    @Override
    @Transactional
    @RedisLock(name = "roster", key = "#memberId")
    public RosterResponse submit(long memberId, RosterSubmitRequest request) {
        LocalDate rosterDate = fantasyTime.now();
        int dateId = DateIds.from(rosterDate);
        if (rosterDao.findByMemberIdAndDateId(memberId, dateId).isPresent())
            throw new RosterAlreadyExistsException();

        Map<Long, PlayerSummary> playerSummaryById = validateAndLoadPlayers(dateId, request.getPlayers());

        // 제출은 기존 로스터가 없으므로, 요청 선수 중 이미 시작한 경기 선수가 있는지만 확인한다.
        validateSubmitStartedGameRestriction(rosterDate, playerSummaryById);

        LocalDateTime now = fantasyTime.getDateTime();
        Roster roster = Roster.builder()
                .memberId(memberId)
                .dateId(dateId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        long rosterId = rosterDao.insert(roster);
        List<RosterPlayer> rosterPlayers = toRosterPlayers(rosterId, request.getPlayers(), playerSummaryById, now);
        rosterPlayerDao.insertAll(rosterPlayers);

        return toResponse(rosterId, rosterDate, request.getPlayers(), playerSummaryById);
    }

    @Override
    @Transactional
    @RedisLock(name = "roster", key = "#memberId")
    public RosterResponse update(long memberId, RosterSubmitRequest request) {
        LocalDate rosterDate = fantasyTime.now();
        int dateId = DateIds.from(rosterDate);
        Roster roster = rosterDao.findByMemberIdAndDateId(memberId, dateId)
                .orElseThrow(RosterNotFoundException::new);

        Map<Long, PlayerSummary> playerSummaryById = validateAndLoadPlayers(dateId, request.getPlayers());

        // 수정은 기존 로스터와 요청 로스터를 비교해, 이미 시작한 경기 선수가 그대로 유지됐는지 확인한다.
        validateUpdateStartedGameRestriction(roster, rosterDate, request.getPlayers(), playerSummaryById);

        LocalDateTime now = fantasyTime.getDateTime();
        List<RosterPlayer> rosterPlayers = toRosterPlayers(
                roster.getRosterId(),
                request.getPlayers(),
                playerSummaryById,
                now);

        // 화면은 14명 전체 구성을 다시 제출하므로 기존 슬롯을 비우고 새 목록으로 교체한다.
        rosterPlayerDao.deleteByRosterId(roster.getRosterId());
        rosterPlayerDao.insertAll(rosterPlayers);
        rosterDao.updateTimestamp(roster.getRosterId(), now);

        return toResponse(roster.getRosterId(), rosterDate, request.getPlayers(), playerSummaryById);
    }

    @Override
    @Transactional(readOnly = true)
    public RosterResponse find(long memberId, LocalDate date) {
        int dateId = DateIds.from(date);

        return rosterDao.findByMemberIdAndDateId(memberId, dateId)
                .map(roster -> toResponse(roster, date))
                .orElse(null);
    }

    private Map<Long, PlayerSummary> validateAndLoadPlayers(int dateId, List<RosterPlayerRequest> players) {
        validateRosterRequest(players);

        Set<Long> playerIds = players.stream()
                .map(RosterPlayerRequest::getPlayerId)
                .collect(Collectors.toSet());

        Map<Long, PlayerSummary> playerSummaryById = playerDao.findSummariesByPlayerIds(dateId, playerIds)
                .stream()
                .collect(Collectors.toMap(PlayerSummary::getPlayerId, Function.identity()));

        if (playerSummaryById.size() != playerIds.size())
            throw new RosterValidationException("선수 정보가 올바르지 않습니다.");

        validateRosterComposition(playerSummaryById.values().stream().toList());

        return playerSummaryById;
    }

    private void validateSubmitStartedGameRestriction(
            LocalDate rosterDate,
            Map<Long, PlayerSummary> requestedPlayerSummaryById) {
        // 제출은 "새 로스터 생성"이라 유지해야 할 기존 선수가 없다.
        // 따라서 요청 선수 중 이미 시작한 팀 소속 선수가 하나라도 있으면 제출 자체를 막는다.
        Set<String> startedTeamCodes = findStartedTeamCodes(rosterDate, requestedPlayerSummaryById.values().stream().toList());

        boolean containsStartedPlayer = requestedPlayerSummaryById.values()
                .stream()
                .map(PlayerSummary::getTeamCode)
                .anyMatch(startedTeamCodes::contains);

        if (containsStartedPlayer)
            throw new RosterValidationException(STARTED_GAME_PLAYER_CHANGE_MESSAGE);
    }

    private void validateUpdateStartedGameRestriction(
            Roster roster,
            LocalDate rosterDate,
            List<RosterPlayerRequest> requestedPlayers,
            Map<Long, PlayerSummary> requestedPlayerSummaryById) {
        // 수정 검증은 DB에 저장된 기존 로스터와 클라이언트가 보낸 변경 로스터를 비교한다.
        List<RosterPlayer> savedPlayers = loadRosterPlayers(roster.getRosterId());
        Map<Long, PlayerSummary> savedPlayerSummaryById = loadPlayerSummaries(roster.getDateId(), savedPlayers);

        // 비교 대상 타입이 서로 달라서 slotNo, playerId, teamCode만 가진 같은 형태로 맞춘다.
        List<RosterSlotPlayer> savedSlotPlayers = toSavedSlotPlayers(savedPlayers, savedPlayerSummaryById);
        List<RosterSlotPlayer> requestedSlotPlayers = toRequestedSlotPlayers(requestedPlayers, requestedPlayerSummaryById);

        // 기존 목록과 요청 목록의 팀을 모두 봐야 제거, 교체, 신규 추가 케이스를 한 번에 판별할 수 있다.
        Set<String> startedTeamCodes = findStartedTeamCodesBySlots(rosterDate, mergeSlotPlayers(savedSlotPlayers, requestedSlotPlayers));

        // 시작한 경기의 선수는 기존 로스터와 요청 로스터에서 같은 슬롯, 같은 선수로 남아 있어야 한다.
        validateStartedGamePlayerChanges(savedSlotPlayers, requestedSlotPlayers, startedTeamCodes);
    }

    private List<RosterPlayer> loadRosterPlayers(long rosterId) {

        return rosterPlayerDao.findByRosterId(rosterId);
    }

    private Map<Long, PlayerSummary> loadPlayerSummaries(int dateId, List<RosterPlayer> rosterPlayers) {
        // 경기 시작 여부는 선수의 당일 팀 코드가 필요해서 roster player만으로는 판단할 수 없다.
        Set<Long> playerIds = rosterPlayers.stream()
                .map(RosterPlayer::getPlayerId)
                .collect(Collectors.toSet());

        return playerDao.findSummariesByPlayerIds(dateId, playerIds)
                .stream()
                .collect(Collectors.toMap(PlayerSummary::getPlayerId, Function.identity()));
    }

    private Set<String> findStartedTeamCodes(LocalDate rosterDate, List<PlayerSummary> playerSummaries) {
        // 선수별로 경기를 직접 들고 있지 않으므로, 당일 팀 코드를 통해 이미 시작한 경기 팀을 찾는다.
        Set<String> teamCodes = playerSummaries.stream()
                .map(PlayerSummary::getTeamCode)
                .collect(Collectors.toSet());

        return gameDao.findStartedTeamCodes(rosterDate, fantasyTime.getDateTime(), teamCodes);
    }

    private Set<String> findStartedTeamCodesBySlots(LocalDate rosterDate, List<RosterSlotPlayer> slotPlayers) {
        // 수정 검증용 비교 모델에서도 팀 코드만 모으면 같은 DAO 쿼리를 재사용할 수 있다.
        Set<String> teamCodes = slotPlayers.stream()
                .map(RosterSlotPlayer::teamCode)
                .collect(Collectors.toSet());

        return gameDao.findStartedTeamCodes(rosterDate, fantasyTime.getDateTime(), teamCodes);
    }

    private void validateStartedGamePlayerChanges(
            List<RosterSlotPlayer> savedPlayers,
            List<RosterSlotPlayer> requestedPlayers,
            Set<String> startedTeamCodes) {
        // slotNo 기준으로 맞춰 봐야 "선수는 남아 있지만 슬롯만 바뀐 경우"까지 잡을 수 있다.
        Map<Integer, RosterSlotPlayer> requestedPlayerBySlotNo = requestedPlayers.stream()
                .collect(Collectors.toMap(RosterSlotPlayer::slotNo, Function.identity()));
        Map<Integer, RosterSlotPlayer> savedPlayerBySlotNo = savedPlayers.stream()
                .collect(Collectors.toMap(RosterSlotPlayer::slotNo, Function.identity()));

        // 기존 시작 경기 선수가 요청에서 빠졌거나 다른 슬롯으로 이동하면 실패한다.
        savedPlayers.forEach(savedPlayer -> {
            if (!startedTeamCodes.contains(savedPlayer.teamCode()))
                return;

            RosterSlotPlayer requestedPlayer = requestedPlayerBySlotNo.get(savedPlayer.slotNo());
            if (requestedPlayer == null || requestedPlayer.playerId() != savedPlayer.playerId())
                throw new RosterValidationException(STARTED_GAME_PLAYER_CHANGE_MESSAGE);
        });

        // 요청에 새로 들어온 선수가 이미 시작한 경기 소속이면, 기존 동일 슬롯 선수일 때만 허용한다.
        requestedPlayers.forEach(requestedPlayer -> {
            if (!startedTeamCodes.contains(requestedPlayer.teamCode()))
                return;

            RosterSlotPlayer savedPlayer = savedPlayerBySlotNo.get(requestedPlayer.slotNo());
            if (savedPlayer == null || savedPlayer.playerId() != requestedPlayer.playerId())
                throw new RosterValidationException(STARTED_GAME_PLAYER_CHANGE_MESSAGE);
        });
    }

    private List<RosterSlotPlayer> toSavedSlotPlayers(
            List<RosterPlayer> savedPlayers,
            Map<Long, PlayerSummary> playerSummaryById) {

        return savedPlayers.stream()
                .map(player -> toRosterSlotPlayer(player.getSlotNo(), player.getPlayerId(), playerSummaryById))
                .toList();
    }

    private List<RosterSlotPlayer> toRequestedSlotPlayers(
            List<RosterPlayerRequest> requestedPlayers,
            Map<Long, PlayerSummary> playerSummaryById) {

        return requestedPlayers.stream()
                .map(player -> toRosterSlotPlayer(player.getSlotNo(), player.getPlayerId(), playerSummaryById))
                .toList();
    }

    private RosterSlotPlayer toRosterSlotPlayer(
            int slotNo,
            long playerId,
            Map<Long, PlayerSummary> playerSummaryById) {
        // 변경 가능 여부 판단에는 화면 응답용 선수 정보가 아니라 당일 팀 코드만 필요하다.
        PlayerSummary summary = playerSummaryById.get(playerId);

        return new RosterSlotPlayer(slotNo, playerId, summary.getTeamCode());
    }

    private List<RosterSlotPlayer> mergeSlotPlayers(
            List<RosterSlotPlayer> savedPlayers,
            List<RosterSlotPlayer> requestedPlayers) {

        return Stream.concat(savedPlayers.stream(), requestedPlayers.stream())
                .toList();
    }

    private void validateRosterRequest(List<RosterPlayerRequest> players) {
        if (players.size() != ROSTER_PLAYER_COUNT)
            throw new RosterValidationException("로스터는 14명으로 구성해야 합니다.");

        long slotCount = players.stream()
                .map(RosterPlayerRequest::getSlotNo)
                .distinct()
                .count();
        if (slotCount != players.size())
            throw new RosterValidationException("로스터 슬롯은 중복될 수 없습니다.");

        long playerCount = players.stream()
                .map(RosterPlayerRequest::getPlayerId)
                .distinct()
                .count();
        if (playerCount != players.size())
            throw new RosterValidationException("같은 선수는 중복 등록할 수 없습니다.");
    }

    private void validateRosterComposition(List<PlayerSummary> playerSummaries) {
        List<PlayerSummary> batters = playerSummaries.stream()
                .filter(player -> player.getPlayerType() == PlayerType.BATTER)
                .toList();
        List<PlayerSummary> pitchers = playerSummaries.stream()
                .filter(player -> player.getPlayerType() == PlayerType.PITCHER)
                .toList();

        if (batters.size() != BATTER_COUNT || pitchers.size() != PITCHER_COUNT)
            throw new RosterValidationException("로스터는 타자 9명, 투수 5명으로 구성해야 합니다.");

        Set<PlayerPosition> batterPositions = batters.stream()
                .map(PlayerSummary::getPositionCode)
                .collect(Collectors.toSet());
        if (!batterPositions.equals(REQUIRED_BATTER_POSITIONS))
            throw new RosterValidationException("타자 포지션 구성이 올바르지 않습니다.");

        boolean allPitchers = pitchers.stream()
                .allMatch(player -> player.getPositionCode() == PlayerPosition.P);
        if (!allPitchers)
            throw new RosterValidationException("투수 포지션 구성이 올바르지 않습니다.");
    }

    private List<RosterPlayer> toRosterPlayers(
            long rosterId,
            List<RosterPlayerRequest> players,
            Map<Long, PlayerSummary> playerSummaryById,
            LocalDateTime now) {

        return players.stream()
                .map(player -> {
                    PlayerSummary summary = playerSummaryById.get(player.getPlayerId());

                    return RosterPlayer.builder()
                            .rosterId(rosterId)
                            .slotNo(player.getSlotNo())
                            .playerId(player.getPlayerId())
                            .playerType(summary.getPlayerType())
                            .positionCode(summary.getPositionCode())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                })
                .toList();
    }

    private RosterResponse toResponse(Roster roster, LocalDate date) {
        List<RosterPlayer> rosterPlayers = rosterPlayerDao.findByRosterId(roster.getRosterId());
        Set<Long> playerIds = rosterPlayers.stream()
                .map(RosterPlayer::getPlayerId)
                .collect(Collectors.toSet());
        Map<Long, PlayerSummary> playerSummaryById = playerDao.findSummariesByPlayerIds(roster.getDateId(), playerIds)
                .stream()
                .collect(Collectors.toMap(PlayerSummary::getPlayerId, Function.identity()));

        List<RosterPlayerResponse> playerResponses = rosterPlayers.stream()
                .map(player -> toPlayerResponse(player.getSlotNo(), playerSummaryById.get(player.getPlayerId())))
                .toList();

        return RosterResponse.builder()
                .rosterId(roster.getRosterId())
                .date(date)
                .players(playerResponses)
                .build();
    }

    private RosterResponse toResponse(
            long rosterId,
            LocalDate date,
            List<RosterPlayerRequest> players,
            Map<Long, PlayerSummary> playerSummaryById) {
        List<RosterPlayerResponse> playerResponses = players.stream()
                .sorted(Comparator.comparing(RosterPlayerRequest::getSlotNo))
                .map(player -> toPlayerResponse(player.getSlotNo(), playerSummaryById.get(player.getPlayerId())))
                .toList();

        return RosterResponse.builder()
                .rosterId(rosterId)
                .date(date)
                .players(playerResponses)
                .build();
    }

    private RosterPlayerResponse toPlayerResponse(int slotNo, PlayerSummary summary) {

        return RosterPlayerResponse.builder()
                .slotNo(slotNo)
                .playerId(summary.getPlayerId())
                .playerName(summary.getPlayerName())
                .playerType(summary.getPlayerType())
                .positionCode(summary.getPositionCode())
                .cost(summary.getCost())
                .point(summary.getPoint())
                .avgPoint(summary.getAvgPoint())
                .build();
    }

    private record RosterSlotPlayer(int slotNo, long playerId, String teamCode) {
    }

}
