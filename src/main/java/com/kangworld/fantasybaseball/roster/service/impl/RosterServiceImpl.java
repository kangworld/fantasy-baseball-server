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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerSummary;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.roster.dao.FantasyRosterDao;
import com.kangworld.fantasybaseball.roster.dao.FantasyRosterPlayerDao;
import com.kangworld.fantasybaseball.roster.exception.RosterAlreadyExistsException;
import com.kangworld.fantasybaseball.roster.exception.RosterNotFoundException;
import com.kangworld.fantasybaseball.roster.exception.RosterValidationException;
import com.kangworld.fantasybaseball.roster.model.FantasyRoster;
import com.kangworld.fantasybaseball.roster.model.FantasyRosterPlayer;
import com.kangworld.fantasybaseball.roster.service.RosterService;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPlayerRequest;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPlayerResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterSubmitRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RosterServiceImpl implements RosterService {

    private static final int ROSTER_PLAYER_COUNT = 14;
    private static final int BATTER_COUNT = 9;
    private static final int PITCHER_COUNT = 5;
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

    private final FantasyRosterDao fantasyRosterDao;
    private final FantasyRosterPlayerDao fantasyRosterPlayerDao;
    private final PlayerDao playerDao;

    @Override
    @Transactional
    public RosterResponse submit(long memberId, RosterSubmitRequest request) {
        LocalDate rosterDate = LocalDate.now();
        int dateId = DateIds.from(rosterDate);
        if (fantasyRosterDao.findByMemberIdAndDateId(memberId, dateId).isPresent())
            throw new RosterAlreadyExistsException();

        Map<Long, PlayerSummary> playerSummaryById = validateAndLoadPlayers(dateId, request.getPlayers());
        LocalDateTime now = LocalDateTime.now();
        FantasyRoster roster = FantasyRoster.builder()
                .memberId(memberId)
                .dateId(dateId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        long rosterId = fantasyRosterDao.insert(roster);
        List<FantasyRosterPlayer> rosterPlayers = toRosterPlayers(rosterId, request.getPlayers(), playerSummaryById, now);
        fantasyRosterPlayerDao.insertAll(rosterPlayers);

        return toResponse(rosterId, rosterDate, request.getPlayers(), playerSummaryById);
    }

    @Override
    @Transactional
    public RosterResponse update(long memberId, RosterSubmitRequest request) {
        LocalDate rosterDate = LocalDate.now();
        int dateId = DateIds.from(rosterDate);
        FantasyRoster roster = fantasyRosterDao.findByMemberIdAndDateId(memberId, dateId)
                .orElseThrow(RosterNotFoundException::new);

        Map<Long, PlayerSummary> playerSummaryById = validateAndLoadPlayers(dateId, request.getPlayers());
        LocalDateTime now = LocalDateTime.now();
        List<FantasyRosterPlayer> rosterPlayers = toRosterPlayers(
                roster.getRosterId(),
                request.getPlayers(),
                playerSummaryById,
                now);

        // 화면은 14명 전체 구성을 다시 제출하므로 기존 슬롯을 비우고 새 목록으로 교체한다.
        fantasyRosterPlayerDao.deleteByRosterId(roster.getRosterId());
        fantasyRosterPlayerDao.insertAll(rosterPlayers);
        fantasyRosterDao.updateTimestamp(roster.getRosterId(), now);

        return toResponse(roster.getRosterId(), rosterDate, request.getPlayers(), playerSummaryById);
    }

    @Override
    @Transactional(readOnly = true)
    public RosterResponse find(long memberId, LocalDate date) {
        int dateId = DateIds.from(date);

        return fantasyRosterDao.findByMemberIdAndDateId(memberId, dateId)
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

    private List<FantasyRosterPlayer> toRosterPlayers(
            long rosterId,
            List<RosterPlayerRequest> players,
            Map<Long, PlayerSummary> playerSummaryById,
            LocalDateTime now) {

        return players.stream()
                .map(player -> {
                    PlayerSummary summary = playerSummaryById.get(player.getPlayerId());

                    return FantasyRosterPlayer.builder()
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

    private RosterResponse toResponse(FantasyRoster roster, LocalDate date) {
        List<FantasyRosterPlayer> rosterPlayers = fantasyRosterPlayerDao.findByRosterId(roster.getRosterId());
        Set<Long> playerIds = rosterPlayers.stream()
                .map(FantasyRosterPlayer::getPlayerId)
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

}
