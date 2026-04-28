package com.kangworld.fantasybaseball.game.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.game.service.GameService;
import com.kangworld.fantasybaseball.game.service.dto.GameScheduleResponse;
import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final KboScheduleFetchService scheduleFetchService;
    private final KboGameRecordFetchService gameRecordFetchService;
    private final FantasyTime fantasyTime;

    @Override
    public List<GameScheduleResponse> findSchedule(LocalDate date, KboGameStatus status) {
        LocalDate targetDate = date == null ? fantasyTime.now() : date;

        return scheduleFetchService.fetch(targetDate).stream()
                .filter(game -> status == null || game.getStatus() == status)
                .map(GameScheduleResponse::from)
                .toList();
    }

    @Override
    public List<PlayerRecordResponse> findTodayPlayerRecords() {
        // 실시간 기록은 당일 경기 중 기록 확인용이므로 record 동기화 대상 경기만 조회한다.
        return scheduleFetchService.findRecordTargetGames(fantasyTime.now()).stream()
                .map(KboScheduleGame::getGameId)
                .map(gameRecordFetchService::fetch)
                .flatMap(this::toPlayerRecords)
                .toList();
    }

    private Stream<PlayerRecordResponse> toPlayerRecords(KboGameRecord gameRecord) {
        Stream<PlayerRecordResponse> batters = gameRecord.getBatterRecords().stream()
                .map(record -> PlayerRecordResponse.fromBatter(gameRecord.getGameId(), record));

        Stream<PlayerRecordResponse> pitchers = gameRecord.getPitcherRecords().stream()
                .map(record -> PlayerRecordResponse.fromPitcher(gameRecord.getGameId(), record));

        return Stream.concat(batters, pitchers);
    }

}
