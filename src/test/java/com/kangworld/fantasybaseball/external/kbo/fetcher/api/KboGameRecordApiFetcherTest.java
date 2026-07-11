package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;

class KboGameRecordApiFetcherTest {

    @Test
    void fetch는_실제_record_응답을_내부_경기기록으로_변환한다() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboGameRecordApiFetcher fetcher = new KboGameRecordApiFetcher(builder.build(), new KboGameStatusMapper());

        server.expect(requestTo("https://example.test/schedule/games/20240421KTLT12024/record"))
                .andRespond(withSuccess(
                        KboResponseFixtures.json("game-record-20240421KTLT12024.json"),
                        MediaType.APPLICATION_JSON
                ));

        KboGameRecord record = fetcher.fetch("20240421KTLT12024");

        // record 응답의 gameInfo에는 gameId가 없으므로 요청 경로의 값을 내부 모델에 보존한다.
        assertThat(record.getGameId()).isEqualTo("20240421KTLT12024");
        assertThat(record.getStatus()).isEqualTo(KboGameStatus.FINISHED);
        assertThat(record.getBatterRecords())
                .filteredOn(batter -> "50054".equals(batter.getPlayerCode()))
                .singleElement()
                .satisfies(batter -> {
                    assertThat(batter.getName()).isEqualTo("천성호");
                    assertThat(batter.getTeamCode()).isEqualTo("KT");
                    assertThat(batter.getPosition()).isEqualTo(PlayerPosition.B2);
                    assertThat(batter.getPaResults())
                            .containsExactly("우비", "우2", "삼진", "좌안", "우비");
                });
        assertThat(record.getBatterRecords())
                .filteredOn(batter -> "50150".equals(batter.getPlayerCode()))
                .singleElement()
                .satisfies(batter -> assertThat(batter.getPosition()).isEqualTo(PlayerPosition.B3));
        assertThat(record.getBatterRecords())
                .filteredOn(batter -> "67006".equals(batter.getPlayerCode()))
                .singleElement()
                .satisfies(batter -> {
                    assertThat(batter.getPos()).isEqualTo("교");
                    assertThat(batter.getPosition()).isEqualTo(PlayerPosition.DH);
                });
        assertThat(record.getPitcherRecords())
                .extracting(pitcher -> pitcher.getTeamCode() + ":" + pitcher.getPlayerCode())
                .containsExactly("KT:69032", "LT:53546");
        assertThat(record.getEtcRecords()).singleElement()
                .satisfies(etc -> assertThat(etc.getHow()).isEqualTo("홈런"));
        server.verify();
    }

    @Test
    void fetch는_record_선수명이_null이어도_나머지_필드를_변환한다() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboGameRecordApiFetcher fetcher = new KboGameRecordApiFetcher(builder.build(), new KboGameStatusMapper());
        String response = KboResponseFixtures.json("game-record-20240421KTLT12024.json")
                .replace("\"name\": \"안치영\"", "\"name\": null");

        server.expect(requestTo("https://example.test/schedule/games/20240421KTLT12024/record"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        KboGameRecord record = fetcher.fetch("20240421KTLT12024");

        assertThat(record.getBatterRecords())
                .filteredOn(batter -> "67006".equals(batter.getPlayerCode()))
                .singleElement()
                .satisfies(batter -> {
                    assertThat(batter.getName()).isNull();
                    assertThat(batter.getPosition()).isEqualTo(PlayerPosition.DH);
                });
        server.verify();
    }

}
