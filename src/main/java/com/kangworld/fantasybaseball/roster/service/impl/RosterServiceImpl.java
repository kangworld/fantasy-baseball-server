package com.kangworld.fantasybaseball.roster.service.impl;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.game.service.GameService;
import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;
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
import com.kangworld.fantasybaseball.roster.service.RosterLineupRules;
import com.kangworld.fantasybaseball.roster.service.RosterLineupRules.RosterPositionRule;
import com.kangworld.fantasybaseball.roster.service.RosterService;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPlayerRequest;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPlayerResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterSubmitRequest;
import com.kangworld.rediscore.lock.annotation.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RosterServiceImpl implements RosterService {

    private final RosterDao rosterDao;
    private final RosterPlayerDao rosterPlayerDao;
    private final PlayerDao playerDao;
    private final KboScheduleFetchService scheduleFetchService;
    private final FantasyTime fantasyTime;
    private final GameService gameService;
    private final RosterLineupRules rosterLineupRules;

    @Override
    @Transactional
    @RedisLock(name = "roster", key = "#memberId")
    public RosterResponse submit(long memberId, RosterSubmitRequest request) {
        LocalDate rosterDate = fantasyTime.now();
        int dateId = DateIds.from(rosterDate);
        LocalDateTime now = fantasyTime.getDateTime();
        // 요청 자체가 잘못됐으면 기존 제출 여부보다 먼저 400으로 응답한다.
        validateRequest(request.getPlayers());

        if (rosterDao.findByMemberIdAndDateId(memberId, dateId).isPresent())
            throw new RosterAlreadyExistsException();

        // 선수 존재 / 라인업 규칙 / 시작 경기 제한을 모두 통과한 뒤에만 roster row를 만든다.
        Map<Long, PlayerSummary> playerSummaryById = getPlayerSummaries(dateId, getRequestedPlayerIds(request.getPlayers()));
        validateLineup(request.getPlayers(), playerSummaryById);
        validateStartedPlayerChange(rosterDate, now, Map.of(), playerSummaryById);

        Roster roster = Roster.builder()
                .memberId(memberId)
                .dateId(dateId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        long rosterId = rosterDao.insert(roster);
        List<RosterPlayer> rosterPlayers = toRosterPlayers(rosterId, request.getPlayers(), now);
        rosterPlayerDao.insertAll(rosterPlayers);

        return toResponse(rosterId, rosterDate, request.getPlayers(), playerSummaryById);
    }

    @Override
    @Transactional
    @RedisLock(name = "roster", key = "#memberId")
    public RosterResponse update(long memberId, RosterSubmitRequest request) {
        LocalDate rosterDate = fantasyTime.now();
        int dateId = DateIds.from(rosterDate);
        LocalDateTime now = fantasyTime.getDateTime();
        // 수정도 제출과 같은 요청 검증 순서를 사용해 잘못된 요청을 먼저 차단한다.
        validateRequest(request.getPlayers());

        Roster roster = rosterDao.findByMemberIdAndDateId(memberId, dateId)
                .orElseThrow(RosterNotFoundException::new);

        // 클라이언트는 14명 전체 라인업을 다시 보내므로 저장 전후의 시작 경기 선수 집합만 비교한다.
        Map<Long, PlayerSummary> playerSummaryById = getPlayerSummaries(dateId, getRequestedPlayerIds(request.getPlayers()));
        validateLineup(request.getPlayers(), playerSummaryById);

        List<RosterPlayer> savedPlayers = rosterPlayerDao.findByRosterId(roster.getRosterId());
        Map<Long, PlayerSummary> savedPlayerSummaryById = getPlayerSummaries(roster.getDateId(), getRosterPlayerIds(savedPlayers));
        validateStartedPlayerChange(rosterDate, now, savedPlayerSummaryById, playerSummaryById);

        List<RosterPlayer> rosterPlayers = toRosterPlayers(
                roster.getRosterId(),
                request.getPlayers(),
                now);

        // 화면은 14명 전체 구성을 다시 제출하므로 기존 포지션 구성을 비우고 새 목록으로 교체한다.
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

    @Override
    @Transactional(readOnly = true)
    public List<PlayerRecordResponse> findMyPlayerRecords(long memberId) {
        int dateId = DateIds.from(fantasyTime.now());

        return rosterDao.findByMemberIdAndDateId(memberId, dateId)
                .map(this::findRosterPlayerRecords)
                .orElse(List.of());
    }

    private List<PlayerRecordResponse> findRosterPlayerRecords(Roster roster) {
        Set<Long> playerIds = rosterPlayerDao.findByRosterId(roster.getRosterId()).stream()
                .map(RosterPlayer::getPlayerId)
                .collect(Collectors.toSet());

        return gameService.findTodayPlayerRecordsByPlayerIds(playerIds);
    }

    private void validateRequest(List<RosterPlayerRequest> players) {
        long positionOrderCount = players.stream()
                .map(player -> player.getRosterPosition().name() + ":" + player.getPositionOrder())
                .distinct()
                .count();
        if (positionOrderCount != players.size())
            throw new RosterValidationException("로스터 포지션 순서는 중복될 수 없습니다.");

        long playerCount = players.stream()
                .map(RosterPlayerRequest::getPlayerId)
                .distinct()
                .count();
        if (playerCount != players.size())
            throw new RosterValidationException("같은 선수는 중복 등록할 수 없습니다.");
    }

    private Set<Long> getRequestedPlayerIds(List<RosterPlayerRequest> players) {
        return players.stream()
                .map(RosterPlayerRequest::getPlayerId)
                .collect(Collectors.toSet());
    }

    private Set<Long> getRosterPlayerIds(List<RosterPlayer> players) {
        return players.stream()
                .map(RosterPlayer::getPlayerId)
                .collect(Collectors.toSet());
    }

    private Map<Long, PlayerSummary> getPlayerSummaries(int dateId, Set<Long> playerIds) {
        Map<Long, PlayerSummary> playerSummaryById = playerDao.findSummariesByPlayerIds(dateId, playerIds)
                .stream()
                .collect(Collectors.toMap(PlayerSummary::getPlayerId, Function.identity()));

        if (playerSummaryById.size() != playerIds.size())
            throw new RosterValidationException("선수 정보가 올바르지 않습니다.");

        return playerSummaryById;
    }

    private void validateLineup(
            List<RosterPlayerRequest> players,
            Map<Long, PlayerSummary> playerSummaryById) {

        // 비용 제한은 클라이언트 표시값이 아니라 당일 player profile의 cost 기준으로 검증한다.
        int totalCost = players.stream()
                .map(RosterPlayerRequest::getPlayerId)
                .map(playerSummaryById::get)
                .mapToInt(PlayerSummary::getCost)
                .sum();
        if (totalCost > rosterLineupRules.getCostLimit())
            throw new RosterValidationException("로스터 비용 제한을 초과했습니다.");

        // 먼저 역할별 정원을 집계해 누락/초과를 한 번에 검증한다.
        Map<PlayerPosition, Long> countByRosterPosition = players.stream()
                .collect(Collectors.groupingBy(RosterPlayerRequest::getRosterPosition, Collectors.counting()));

        rosterLineupRules.getPositionRules().forEach(rule -> {
            long count = countByRosterPosition.getOrDefault(rule.rosterPosition(), 0L);
            if (count != rule.count())
                throw new RosterValidationException("로스터 포지션 구성이 올바르지 않습니다.");
        });

        players.forEach(player -> {
            RosterPositionRule rule = rosterLineupRules.getPositionRule(player.getRosterPosition());
            PlayerSummary summary = playerSummaryById.get(player.getPlayerId());

            // positionOrder는 같은 포지션 안에서의 순번이다. P는 1~5, 나머지는 1만 허용한다.
            if (player.getPositionOrder() < 1 || player.getPositionOrder() > rule.count())
                throw new RosterValidationException("로스터 포지션 순서가 올바르지 않습니다.");

            // 라인업 칸의 선수 유형과 실제 선수 유형이 다르면 클라이언트 필터 우회 요청으로 본다.
            if (player.isPitcher() != (summary.getPlayerType() == PlayerType.PITCHER))
                throw new RosterValidationException("로스터 포지션에 등록할 수 없는 선수 유형입니다.");

            // DH는 타자 포지션 전체를 허용하고, 나머지 포지션은 동일 포지션 선수만 허용한다.
            if (!isAllowedPosition(player.getRosterPosition(), summary.getPositionCode()))
                throw new RosterValidationException("로스터 포지션에 등록할 수 없는 선수 포지션입니다.");
        });
    }

    private boolean isAllowedPosition(PlayerPosition rosterPosition, PlayerPosition positionCode) {
        if (rosterPosition == PlayerPosition.DH)
            return positionCode != PlayerPosition.P;

        return rosterPosition == positionCode;
    }

    private void validateStartedPlayerChange(
            LocalDate rosterDate,
            LocalDateTime now,
            Map<Long, PlayerSummary> savedPlayerSummaryById,
            Map<Long, PlayerSummary> requestedPlayerSummaryById) {
        // 시작 경기 여부는 기존 선수와 요청 선수를 모두 봐야 한다.
        // 기존 선수만 보면 시작 경기 선수를 새로 추가하는 요청을 놓치고,
        // 요청 선수만 보면 기존 시작 경기 선수를 제거한 요청을 놓친다.
        List<PlayerSummary> checkTargets = Stream.concat(
                savedPlayerSummaryById.values().stream(),
                requestedPlayerSummaryById.values().stream()
        ).toList();

        Set<String> startedTeamCodes = findStartedTeamCodes(rosterDate, now, checkTargets);
        Set<Long> savedStartedPlayerIds = findStartedPlayerIds(savedPlayerSummaryById, startedTeamCodes);
        Set<Long> requestedStartedPlayerIds = findStartedPlayerIds(requestedPlayerSummaryById, startedTeamCodes);

        // 이미 시작한 경기 선수 집합은 저장 전후로 같아야 한다.
        // 제출은 saved 집합이 비어 있으므로 시작 경기 선수가 요청에 포함되면 실패한다.
        // 수정은 시작 경기 선수를 제거하거나 다른 선수로 바꾸면 두 집합이 달라져 실패한다.
        // 더블헤더는 같은 팀 경기 중 하나라도 시작했으면 해당 팀 선수 전체를 시작 경기 선수로 본다.
        if (!savedStartedPlayerIds.equals(requestedStartedPlayerIds))
            throw new RosterValidationException("이미 시작한 경기의 선수는 변경할 수 없습니다.");
    }

    private Set<String> findStartedTeamCodes(
            LocalDate rosterDate,
            LocalDateTime now,
            List<PlayerSummary> playerSummaries) {
        // 선수별로 경기를 직접 들고 있지 않으므로, 당일 팀 코드를 통해 이미 시작한 경기 팀을 찾는다.
        Set<String> teamCodes = playerSummaries.stream()
                .map(PlayerSummary::getTeamCode)
                .collect(Collectors.toSet());

        // 시작 시간이 지난 경기는 이후 취소/중지 상태로 바뀌어도 선수 교체를 허용하지 않는다.
        return scheduleFetchService.fetch(rosterDate).stream()
                .filter(game -> game.getStartAt() != null && !game.getStartAt().isAfter(now))
                .flatMap(game -> Stream.of(game.getHomeTeamCode(), game.getAwayTeamCode()))
                .filter(teamCodes::contains)
                .collect(Collectors.toSet());
    }

    private Set<Long> findStartedPlayerIds(
            Map<Long, PlayerSummary> playerSummaryById,
            Set<String> startedTeamCodes) {
        return playerSummaryById.values().stream()
                .filter(player -> startedTeamCodes.contains(player.getTeamCode()))
                .map(PlayerSummary::getPlayerId)
                .collect(Collectors.toSet());
    }

    private List<RosterPlayer> toRosterPlayers(
            long rosterId,
            List<RosterPlayerRequest> players,
            LocalDateTime now) {

        return players.stream()
                .map(player -> RosterPlayer.builder()
                        .rosterId(rosterId)
                        .rosterPosition(player.getRosterPosition())
                        .positionOrder(player.getPositionOrder())
                        .playerId(player.getPlayerId())
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();
    }

    private RosterResponse toResponse(Roster roster, LocalDate date) {
        List<RosterPlayer> rosterPlayers = sortRosterPlayers(rosterPlayerDao.findByRosterId(roster.getRosterId()));
        Set<Long> playerIds = rosterPlayers.stream()
                .map(RosterPlayer::getPlayerId)
                .collect(Collectors.toSet());
        Map<Long, PlayerSummary> playerSummaryById = playerDao.findSummariesByPlayerIds(roster.getDateId(), playerIds)
                .stream()
                .collect(Collectors.toMap(PlayerSummary::getPlayerId, Function.identity()));

        List<RosterPlayerResponse> playerResponses = rosterPlayers.stream()
                .map(player -> toPlayerResponse(player.getRosterPosition(), player.getPositionOrder(), playerSummaryById.get(player.getPlayerId())))
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
                .sorted(Comparator
                        .<RosterPlayerRequest>comparingInt(player -> rosterLineupRules.getDisplayOrder(player.getRosterPosition()))
                        .thenComparing(RosterPlayerRequest::getPositionOrder))
                .map(player -> toPlayerResponse(player.getRosterPosition(), player.getPositionOrder(), playerSummaryById.get(player.getPlayerId())))
                .toList();

        return RosterResponse.builder()
                .rosterId(rosterId)
                .date(date)
                .players(playerResponses)
                .build();
    }

    private RosterPlayerResponse toPlayerResponse(
            PlayerPosition rosterPosition,
            int positionOrder,
            PlayerSummary summary) {

        return RosterPlayerResponse.builder()
                .rosterPosition(rosterPosition)
                .positionOrder(positionOrder)
                .playerId(summary.getPlayerId())
                .playerName(summary.getPlayerName())
                .playerType(summary.getPlayerType())
                .positionCode(summary.getPositionCode())
                .cost(summary.getCost())
                .point(summary.getPoint())
                .avgPoint(summary.getAvgPoint())
                .imgPath(summary.getImgPath())
                .build();
    }

    private List<RosterPlayer> sortRosterPlayers(List<RosterPlayer> players) {
        // DB에는 P1, P2 같은 물리 슬롯을 저장하지 않으므로 응답 정렬은 서버 규칙으로 고정한다.
        return players.stream()
                .sorted(Comparator
                        .<RosterPlayer>comparingInt(player -> rosterLineupRules.getDisplayOrder(player.getRosterPosition()))
                        .thenComparing(RosterPlayer::getPositionOrder))
                .toList();
    }

}
