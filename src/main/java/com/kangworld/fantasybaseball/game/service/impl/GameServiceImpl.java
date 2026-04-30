package com.kangworld.fantasybaseball.game.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboLiveGameStateFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.game.service.GameService;
import com.kangworld.fantasybaseball.game.service.dto.GameLiveStateResponse;
import com.kangworld.fantasybaseball.game.service.dto.GameScheduleResponse;
import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final KboScheduleFetchService scheduleFetchService;
    private final KboGameRecordFetchService gameRecordFetchService;
    private final KboLiveGameStateFetchService liveGameStateFetchService;
    private final FantasyTime fantasyTime;

    @Override
    public List<GameScheduleResponse> findSchedule(LocalDate date) {
        LocalDate targetDate = date == null ? fantasyTime.now() : date;

        return scheduleFetchService.fetch(targetDate).stream()
                .map(GameScheduleResponse::from)
                .toList();
    }

    @Override
    public List<GameLiveStateResponse> findLiveStates() {
        // 실시간 상태 화면은 진행 중 경기만이 아니라 오늘 일정의 취소/종료/예정 상태도 보여준다.
        return scheduleFetchService.fetch(fantasyTime.now()).stream()
                .map(KboScheduleGame::getGameId)
                .map(this::findLiveState)
                .toList();
    }

    @Override
    public GameLiveStateResponse findLiveState(String gameId) {
        return GameLiveStateResponse.from(liveGameStateFetchService.fetch(gameId));
    }

    @Override
    public List<PlayerRecordResponse> findTodayPlayerRecords() {
        return findTodayPlayerRecords(record -> true);
    }

    @Override
    public List<PlayerRecordResponse> findTodayPlayerRecordsByPlayerIds(Set<Long> playerIds) {
        if (playerIds.isEmpty())
            return List.of();

        return findTodayPlayerRecords(record -> playerIds.contains(record.getPlayerId()));
    }

    private List<PlayerRecordResponse> findTodayPlayerRecords(Predicate<PlayerRecordResponse> filter) {
        // 선수 기록 조회는 화면 확인용이라 진행/종료/중단 경기까지 허용하고, 정산 필터는 배치에서 별도로 적용한다.
        return scheduleFetchService.fetch(fantasyTime.now()).stream()
                .filter(this::supportsPlayerRecordLookup)
                .map(KboScheduleGame::getGameId)
                .map(gameRecordFetchService::fetch)
                .flatMap(this::toPlayerRecords)
                .filter(filter)
                .toList();
    }

    private boolean supportsPlayerRecordLookup(KboScheduleGame game) {
        KboGameStatus status = game.getStatus();

        return status == KboGameStatus.PLAYING || status == KboGameStatus.FINISHED || status == KboGameStatus.SUSPENDED;
    }

    private Stream<PlayerRecordResponse> toPlayerRecords(KboGameRecord gameRecord) {
        Stream<PlayerRecordResponse> batters = gameRecord.getBatterRecords().stream()
                .map(record -> PlayerRecordResponse.fromBatter(gameRecord.getGameId(), record));

        Stream<PlayerRecordResponse> pitchers = gameRecord.getPitcherRecords().stream()
                .map(record -> PlayerRecordResponse.fromPitcher(gameRecord.getGameId(), record));

        return Stream.concat(batters, pitchers);
    }

}
