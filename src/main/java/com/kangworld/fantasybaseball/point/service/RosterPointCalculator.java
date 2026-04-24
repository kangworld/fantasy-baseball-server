package com.kangworld.fantasybaseball.point.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.roster.dao.FantasyRosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.FantasyRosterPlayer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RosterPointCalculator {

    private final FantasyRosterPlayerDao fantasyRosterPlayerDao;
    private final PlayerDailyPointDao playerDailyPointDao;

    public int calculate(int dateId, long rosterId) {
        List<FantasyRosterPlayer> rosterPlayers = fantasyRosterPlayerDao.findByRosterId(rosterId);
        Set<Long> playerIds = rosterPlayers.stream()
                .map(FantasyRosterPlayer::getPlayerId)
                .collect(Collectors.toSet());

        return playerDailyPointDao.sumPointByPlayerIds(dateId, playerIds);
    }

}
