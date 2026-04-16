package com.kangworld.fantasybaseball.external.kbo.fetcher;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KboGameRecordApiFetcher implements KboGameRecordFetcher {

    private final RestClient kboRestClient;
    private final KboGameStatusMapper gameStatusMapper;

    @Override
    public KboGameRecord fetch(String gameId) {
        GameRecordResponse response = kboRestClient.get()
                .uri("/schedule/games/{gameId}/record", gameId)
                .retrieve()
                .body(GameRecordResponse.class);

        if (response == null || response.result == null || response.result.recordData == null) {
            return KboGameRecord.builder()
                    .gameId(gameId)
                    .status(KboGameStatus.UNKNOWN)
                    .batterRecords(List.of())
                    .pitcherRecords(List.of())
                    .build();
        }

        RecordDataResponse recordData = response.result.recordData;
        List<BatterRecordResponse> batters = merge(recordData.battersBoxscore.away, recordData.battersBoxscore.home);
        List<PitcherRecordResponse> pitchers = merge(recordData.pitchersBoxscore.away, recordData.pitchersBoxscore.home);

        return KboGameRecord.builder()
                .gameId(recordData.gameInfo.gameId)
                .status(gameStatusMapper.map(recordData.gameInfo.statusCode, false, false))
                .batterRecords(batters.stream().map(this::toBatterRecord).toList())
                .pitcherRecords(pitchers.stream().map(this::toPitcherRecord).toList())
                .build();
    }

    private KboBatterRecord toBatterRecord(BatterRecordResponse batter) {
        return KboBatterRecord.builder()
                .playerCode(batter.playerCode)
                .name(batter.name)
                .pos(batter.pos)
                .ab(batter.ab)
                .hit(batter.hit)
                .rbi(batter.rbi)
                .run(batter.run)
                .bb(batter.bb)
                .kk(batter.kk)
                .hr(batter.hr)
                .sb(batter.sb)
                .build();
    }

    private KboPitcherRecord toPitcherRecord(PitcherRecordResponse pitcher) {
        return KboPitcherRecord.builder()
                .playerCode(pitcher.playerCode)
                .name(pitcher.name)
                .inn(pitcher.inn)
                .hit(pitcher.hit)
                .r(pitcher.r)
                .er(pitcher.er)
                .bb(pitcher.bb)
                .kk(pitcher.kk)
                .hr(pitcher.hr)
                .bf(pitcher.bf)
                .build();
    }

    private <T> List<T> merge(List<T> away, List<T> home) {
        List<T> merged = new ArrayList<>();

        if (away != null) {
            merged.addAll(away);
        }

        if (home != null) {
            merged.addAll(home);
        }

        return List.copyOf(merged);
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
        private BattersBoxScoreResponse battersBoxscore;
        private PitchersBoxScoreResponse pitchersBoxscore;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GameInfoResponse {
        private String gameId;
        private String statusCode;
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
        @JsonProperty("r")
        private int r;
        private int er;
        private int bb;
        private int kk;
        private int hr;
        private int bf;
    }

}
