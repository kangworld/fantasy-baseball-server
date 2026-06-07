package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboGameRecordFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboEtcRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KboGameRecordApiFetcher implements KboGameRecordFetcher {

    private static final Pattern INNING_RESULT_FIELD_PATTERN = Pattern.compile("inn\\d+");

    private final RestClient kboRestClient;
    private final KboGameStatusMapper gameStatusMapper;

    public KboGameRecordApiFetcher(
            @Qualifier("kboRestClient") RestClient kboRestClient,
            KboGameStatusMapper gameStatusMapper) {
        this.kboRestClient = kboRestClient;
        this.gameStatusMapper = gameStatusMapper;
    }

    @Override
    public KboGameRecord fetch(String gameId) {
        // 경기별 상세 박스스코어 조회
        GameRecordResponse response = kboRestClient.get()
                .uri("/schedule/games/{gameId}/record", gameId)
                .retrieve()
                .body(GameRecordResponse.class);

        if (response.result == null || response.result.recordData == null) {
            log.warn("KBO game record data is empty. gameId={}", gameId);

            return KboGameRecord.builder()
                    .gameId(gameId)
                    .status(KboGameStatus.UNKNOWN)
                    .batterRecords(List.of())
                    .pitcherRecords(List.of())
                    .etcRecords(List.of())
                    .build();
        }

        RecordDataResponse recordData = response.result.recordData;
        if (recordData.battersBoxscore == null)
            log.warn("KBO batter boxscore is empty. gameId={}", gameId);
        if (recordData.pitchersBoxscore == null)
            log.warn("KBO pitcher boxscore is empty. gameId={}", gameId);

        // 홈/원정 병합 전에 팀 코드를 붙여야 신규 선수 프로필 생성 시 team_code를 보존할 수 있다.
        List<KboBatterRecord> batters = new ArrayList<>();
        batters.addAll(toBatterRecords(recordData.battersBoxscore == null ? List.of() : recordData.battersBoxscore.away, recordData.gameInfo.aCode));
        batters.addAll(toBatterRecords(recordData.battersBoxscore == null ? List.of() : recordData.battersBoxscore.home, recordData.gameInfo.hCode));

        List<KboPitcherRecord> pitchers = new ArrayList<>();
        pitchers.addAll(toPitcherRecords(recordData.pitchersBoxscore == null ? List.of() : recordData.pitchersBoxscore.away, recordData.gameInfo.aCode));
        pitchers.addAll(toPitcherRecords(recordData.pitchersBoxscore == null ? List.of() : recordData.pitchersBoxscore.home, recordData.gameInfo.hCode));

        return KboGameRecord.builder()
                .gameId(recordData.gameInfo.gameId)
                .status(gameStatusMapper.map(recordData.gameInfo.statusCode, false, false))
                .batterRecords(List.copyOf(batters))
                .pitcherRecords(List.copyOf(pitchers))
                .etcRecords(toEtcRecords(recordData.etcRecords))
                .build();
    }

    private List<KboBatterRecord> toBatterRecords(List<BatterRecordResponse> records, String teamCode) {
        if (records == null)
            return List.of();

        return records.stream()
                .map(record -> toBatterRecord(record, teamCode))
                .toList();
    }

    private KboBatterRecord toBatterRecord(BatterRecordResponse batter, String teamCode) {
        return KboBatterRecord.builder()
                .playerCode(batter.playerCode)
                .name(batter.name)
                .teamCode(teamCode)
                .pos(batter.pos)
                .position(toPlayerPosition(batter.pos))
                .ab(batter.ab)
                .hit(batter.hit)
                .rbi(batter.rbi)
                .run(batter.run)
                .bb(batter.bb)
                .kk(batter.kk)
                .hr(batter.hr)
                .sb(batter.sb)
                .paResults(batter.paResults())
                .build();
    }

    private PlayerPosition toPlayerPosition(String value) {
        if (value == null || value.isBlank())
            return PlayerPosition.DH;

        // 네이버 record의 pos는 "주중"처럼 교체 역할과 최종 수비 위치가 붙을 수 있어 마지막 글자를 기준으로 해석한다.
        String trimmed = value.trim();
        String last = trimmed.substring(trimmed.length() - 1);

        return switch (last) {
            case "투" -> PlayerPosition.P;
            case "포" -> PlayerPosition.C;
            case "一" -> PlayerPosition.B1;
            case "二" -> PlayerPosition.B2;
            case "三" -> PlayerPosition.B3;
            case "유" -> PlayerPosition.SS;
            case "좌" -> PlayerPosition.LF;
            case "중" -> PlayerPosition.CF;
            case "우" -> PlayerPosition.RF;
            case "지" -> PlayerPosition.DH;
            default -> PlayerPosition.DH;
        };
    }

    private List<KboPitcherRecord> toPitcherRecords(List<PitcherRecordResponse> records, String teamCode) {
        if (records == null)
            return List.of();

        return records.stream()
                .map(record -> toPitcherRecord(record, teamCode))
                .toList();
    }

    private KboPitcherRecord toPitcherRecord(PitcherRecordResponse pitcher, String teamCode) {
        return KboPitcherRecord.builder()
                .playerCode(pitcher.playerCode)
                .name(pitcher.name)
                .teamCode(teamCode)
                .position(PlayerPosition.P)
                .inn(pitcher.inn)
                .hit(pitcher.hit)
                .r(pitcher.r)
                .er(pitcher.er)
                .bb(pitcher.bb)
                .bbhp(pitcher.bbhp)
                .kk(pitcher.kk)
                .hr(pitcher.hr)
                .bf(pitcher.bf)
                .wls(pitcher.wls)
                .build();
    }

    private List<KboEtcRecord> toEtcRecords(List<EtcRecordResponse> records) {
        // 박스스코어 명시 필드가 없는 이벤트 보조 원천
        if (records == null)
            return List.of();

        return records.stream()
                .map(record -> KboEtcRecord.builder()
                        .how(record.how)
                        .result(record.result)
                        .build())
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GameRecordResponse {
        private GameRecordResult result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GameRecordResult {
        private RecordDataResponse recordData;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RecordDataResponse {
        private GameInfoResponse gameInfo;
        private List<EtcRecordResponse> etcRecords;
        private BattersBoxScoreResponse battersBoxscore;
        private PitchersBoxScoreResponse pitchersBoxscore;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GameInfoResponse {
        private String gameId;
        private String statusCode;
        private String aCode;
        private String hCode;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BattersBoxScoreResponse {
        private List<BatterRecordResponse> away;
        private List<BatterRecordResponse> home;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BatterRecordResponse {
        private String playerCode;
        private String name;
        private String pos;
        private int ab;
        private int hit;
        private int rbi;
        private int run;
        private int bb;
        private int kk;
        private int hr;
        private int sb;
        private final Map<Integer, String> inningResultByNumber = new TreeMap<>();

        @JsonAnySetter
        public void setDynamicField(String fieldName, Object value) {
            // 동적 inn 필드로 타석 순서 유지
            if (!INNING_RESULT_FIELD_PATTERN.matcher(fieldName).matches() || value == null)
                return;

            String result = value.toString();
            if (result.isBlank())
                return;

            inningResultByNumber.put(Integer.parseInt(fieldName.substring(3)), result);
        }

        private List<String> paResults() {
            return inningResultByNumber.entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getKey))
                    .map(Map.Entry::getValue)
                    .toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PitchersBoxScoreResponse {
        private List<PitcherRecordResponse> away;
        private List<PitcherRecordResponse> home;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PitcherRecordResponse {
        @JsonProperty("pcode")
        private String playerCode;
        private String name;
        private String inn;
        private int hit;
        private int r;
        private int er;
        private int bb;
        private int bbhp;
        private int kk;
        private int hr;
        private int bf;
        private String wls;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EtcRecordResponse {
        private String how;
        private String result;
    }

}
