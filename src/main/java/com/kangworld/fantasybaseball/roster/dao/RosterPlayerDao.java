package com.kangworld.fantasybaseball.roster.dao;

import java.util.List;
import java.util.Set;

import com.kangworld.fantasybaseball.roster.model.RosterPlayer;

public interface RosterPlayerDao {

    void insertAll(List<RosterPlayer> players);

    List<RosterPlayer> findByRosterId(long rosterId);

    List<RosterPlayer> findByRosterIds(Set<Long> rosterIds);

    void deleteByRosterId(long rosterId);

}
