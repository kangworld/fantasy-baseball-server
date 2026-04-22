package com.kangworld.fantasybaseball.game.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public interface GameDao {

    Set<String> findStartedTeamCodes(LocalDate date, LocalDateTime now, Set<String> teamCodes);

}
