package com.kangworld.fantasybaseball.roster.dao;

import java.time.LocalDateTime;
import java.util.Optional;

import com.kangworld.fantasybaseball.roster.model.FantasyRoster;

public interface FantasyRosterDao {

    long insert(FantasyRoster roster);

    Optional<FantasyRoster> findByMemberIdAndDateId(long memberId, int dateId);

    void updateTimestamp(long rosterId, LocalDateTime updatedAt);

}
