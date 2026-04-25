package com.kangworld.fantasybaseball.roster.dao;

import java.time.LocalDateTime;
import java.util.Optional;

import com.kangworld.fantasybaseball.roster.model.Roster;

public interface RosterDao {

    long insert(Roster roster);

    Optional<Roster> findByMemberIdAndDateId(long memberId, int dateId);

    void updateTimestamp(long rosterId, LocalDateTime updatedAt);

}
