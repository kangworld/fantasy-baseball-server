package com.kangworld.fantasybaseball.point.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RosterPointCalculator {

    private final RosterPlayerDao rosterPlayerDao;
    private final PlayerDailyPointDao playerDailyPointDao;

    public int calculate(int dateId, long rosterId) {
        List<RosterPlayer> rosterPlayers = rosterPlayerDao.findByRosterId(rosterId);
        Set<Long> playerIds = rosterPlayers.stream()
                .map(RosterPlayer::getPlayerId)
                .collect(Collectors.toSet());

        return playerDailyPointDao.sumPointByPlayerIds(dateId, playerIds);
    }

}
