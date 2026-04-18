package com.kangworld.fantasybaseball.game.service;

import java.time.LocalDate;
import java.util.List;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.game.service.dto.GameScheduleResponse;
import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;

public interface GameService {

    List<GameScheduleResponse> findSchedule(LocalDate date, KboGameStatus status);

    List<PlayerRecordResponse> findTodayPlayerRecords();

}
