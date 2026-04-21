package com.kangworld.fantasybaseball.roster.dao;

import java.util.List;

import com.kangworld.fantasybaseball.roster.model.FantasyRosterPlayer;

public interface FantasyRosterPlayerDao {

    void insertAll(List<FantasyRosterPlayer> players);

    List<FantasyRosterPlayer> findByRosterId(long rosterId);

    void deleteByRosterId(long rosterId);

}
