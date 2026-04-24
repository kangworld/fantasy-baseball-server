package com.kangworld.fantasybaseball.point.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboEtcRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.point.model.BatterGameRecord;
import com.kangworld.fantasybaseball.point.model.PitcherGameRecord;

@Component
public class PlayerGameRecordMapper {

    public List<BatterGameRecord> toBatterRecords(KboGameRecord gameRecord) {
        // 외부 KBO 타자 DTO를 포인트 계산 전용 내부 모델로 변환
        return gameRecord.getBatterRecords().stream()
                .map(record -> toBatterRecord(gameRecord, record))
                .toList();
    }

    public List<PitcherGameRecord> toPitcherRecords(KboGameRecord gameRecord) {
        // 외부 KBO 투수 DTO를 포인트 계산 전용 내부 모델로 변환
        return gameRecord.getPitcherRecords().stream()
                .map(record -> toPitcherRecord(gameRecord, record))
                .toList();
    }

    private BatterGameRecord toBatterRecord(KboGameRecord gameRecord, KboBatterRecord record) {
        return BatterGameRecord.builder()
                .gameId(gameRecord.getGameId())
                .playerId(parsePlayerId(record.getPlayerCode()))
                .playerName(record.getName())
                .pos(record.getPos())
                .ab(record.getAb())
                .hit(record.getHit())
                .rbi(record.getRbi())
                .run(record.getRun())
                .bb(record.getBb())
                .kk(record.getKk())
                .hr(record.getHr())
                .sb(record.getSb())
                .paResults(record.getPaResults())
                // etcRecords 매핑
                .cs(countEtcRecord(gameRecord.getEtcRecords(), KboEtcRecordType.CS, record.getName()))
                .runnerOut(countEtcRecord(gameRecord.getEtcRecords(), KboEtcRecordType.RUNNER_OUT, record.getName()))
                .build();
    }

    private PitcherGameRecord toPitcherRecord(KboGameRecord gameRecord, KboPitcherRecord record) {
        return PitcherGameRecord.builder()
                .gameId(gameRecord.getGameId())
                .playerId(parsePlayerId(record.getPlayerCode()))
                .playerName(record.getName())
                .inn(record.getInn())
                .hit(record.getHit())
                .r(record.getR())
                .er(record.getEr())
                .bb(record.getBb())
                .bbhp(record.getBbhp())
                .kk(record.getKk())
                .hr(record.getHr())
                .bf(record.getBf())
                .wls(record.getWls())
                // etcRecords 매핑
                .wildPitch(countEtcRecord(gameRecord.getEtcRecords(), KboEtcRecordType.WILD_PITCH, record.getName()))
                .balk(countEtcRecord(gameRecord.getEtcRecords(), KboEtcRecordType.BALK, record.getName()))
                .build();
    }

    private int countEtcRecord(List<KboEtcRecord> etcRecords, KboEtcRecordType recordType, String playerName) {
        // etcRecords는 이름 기반 매핑만 가능
        if (etcRecords == null || playerName == null || playerName.isBlank())
            return 0;

        return etcRecords.stream()
                .filter(record -> recordType.matches(record.getHow()))
                .mapToInt(record -> countOccurrences(record.getResult(), playerName))
                .sum();
    }

    private int countOccurrences(String source, String target) {
        // 등장 횟수만큼 계산
        if (source == null || source.isBlank())
            return 0;

        int count = 0;
        int fromIndex = 0;
        while (fromIndex < source.length()) {
            int foundIndex = source.indexOf(target, fromIndex);
            if (foundIndex < 0)
                return count;

            count++;
            fromIndex = foundIndex + target.length();
        }

        return count;
    }

    private long parsePlayerId(String playerCode) {
        // playerCode/pcode 기반 player_id
        if (playerCode == null || playerCode.isBlank())
            return 0;

        return Long.parseLong(playerCode);
    }

    // KBO how 문자열 매핑
    private enum KboEtcRecordType {
        CS("도루자"),
        RUNNER_OUT("주루사"),
        WILD_PITCH("폭투"),
        BALK("보크");

        private final String how;

        KboEtcRecordType(String how) {
            this.how = how;
        }

        private boolean matches(String how) {
            return this.how.equals(how);
        }
    }

}
