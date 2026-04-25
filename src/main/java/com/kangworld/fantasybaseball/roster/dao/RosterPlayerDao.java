package com.kangworld.fantasybaseball.roster.dao;

import java.util.List;

import com.kangworld.fantasybaseball.roster.model.RosterPlayer;

public interface RosterPlayerDao {

    void insertAll(List<RosterPlayer> players);

    List<RosterPlayer> findByRosterId(long rosterId);

    void deleteByRosterId(long rosterId);

}
