package com.kangworld.fantasybaseball.roster.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.kangworld.fantasybaseball.roster.model.Roster;

public interface RosterDao {

    long insert(Roster roster);

    Optional<Roster> findByMemberIdAndDateId(long memberId, int dateId);

    List<Roster> findByDateId(int dateId);

    boolean advanceVersion(long rosterId, long expectedVersion, LocalDateTime updatedAt);

}
