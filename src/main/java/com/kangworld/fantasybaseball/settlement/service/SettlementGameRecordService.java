package com.kangworld.fantasybaseball.settlement.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementGameRecordService {

    private final KboScheduleFetchService scheduleFetchService;
    private final KboGameRecordFetchService gameRecordFetchService;

    public List<KboGameRecord> fetch(LocalDate gameDate) {
        return scheduleFetchService.fetchFresh(gameDate).stream()
                .filter(game -> game.getStatus() != KboGameStatus.CANCELED)
                .map(this::fetchRecord)
                .toList();
    }

    private KboGameRecord fetchRecord(KboScheduleGame game) {
        KboGameStatus status = game.getStatus();
        if (status != KboGameStatus.FINISHED && status != KboGameStatus.SUSPENDED) {
            throw new IllegalStateException("정산 대상 경기의 상태가 확정되지 않았습니다. gameId=" + game.getGameId());
        }

        KboGameRecord record = gameRecordFetchService.fetchFresh(game.getGameId());
        // 일정은 먼저 종료로 바뀔 수 있으므로 기록 API의 상태와 박스스코어도 확인한다.
        boolean recordIsSettled = record.getStatus() == KboGameStatus.FINISHED
                || (status == KboGameStatus.SUSPENDED && record.getStatus() == KboGameStatus.SUSPENDED);
        if (!recordIsSettled || !record.isCompleteBoxScore()
                || record.getBatterRecords().isEmpty() || record.getPitcherRecords().isEmpty()) {
            throw new IllegalStateException("정산 대상 경기의 기록이 아직 준비되지 않았습니다. gameId=" + game.getGameId());
        }
        return record;
    }
}
