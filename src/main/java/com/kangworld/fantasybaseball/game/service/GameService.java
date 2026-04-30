package com.kangworld.fantasybaseball.game.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.kangworld.fantasybaseball.game.service.dto.GameLiveStateResponse;
import com.kangworld.fantasybaseball.game.service.dto.GameScheduleResponse;
import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;

public interface GameService {

    List<GameScheduleResponse> findSchedule(LocalDate date);

    List<GameLiveStateResponse> findLiveStates();

    GameLiveStateResponse findLiveState(String gameId);

    List<PlayerRecordResponse> findTodayPlayerRecords();

    List<PlayerRecordResponse> findTodayPlayerRecordsByPlayerIds(Set<Long> playerIds);

}
