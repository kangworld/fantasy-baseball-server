package com.kangworld.fantasybaseball.point.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RosterPointCalculator {

    private final RosterPlayerDao rosterPlayerDao;
    private final PlayerDailyPointDao playerDailyPointDao;

    public Map<Long, Integer> calculateAll(int dateId, List<Roster> rosters) {
        Set<Long> rosterIds = rosters.stream()
                .map(Roster::getRosterId)
                .collect(Collectors.toSet());
        List<RosterPlayer> rosterPlayers = rosterPlayerDao.findByRosterIds(rosterIds);
        Set<Long> playerIds = rosterPlayers.stream()
                .map(RosterPlayer::getPlayerId)
                .collect(Collectors.toSet());
        Map<Long, Integer> pointByPlayerId = playerDailyPointDao.findPointByPlayerIds(dateId, playerIds);

        // 정산 배치는 로스터 수만큼 DB를 치지 않고, 선수 점수를 메모리에서 로스터별로 합산한다.
        return rosterPlayers.stream()
                .collect(Collectors.groupingBy(
                        RosterPlayer::getRosterId,
                        Collectors.summingInt(player -> pointByPlayerId.getOrDefault(player.getPlayerId(), 0))
                ));
    }

}
