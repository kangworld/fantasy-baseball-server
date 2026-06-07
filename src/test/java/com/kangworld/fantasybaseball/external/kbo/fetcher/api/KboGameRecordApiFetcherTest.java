package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;

class KboGameRecordApiFetcherTest {

    @Test
    void fetch는_recordData_타자_투수_박스스코어를_역직렬화한다() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboGameRecordApiFetcher fetcher = new KboGameRecordApiFetcher(builder.build(), new KboGameStatusMapper());

        server.expect(requestTo("https://example.test/schedule/games/20260606KTSK02026/record"))
                .andRespond(withSuccess("""
                        {
                          "result": {
                            "recordData": {
                              "gameInfo": {
                                "statusCode": "RESULT",
                                "aCode": "KT",
                                "hCode": "SK"
                              },
                              "battersBoxscore": {
                                "away": [
                                  {
                                    "playerCode": "76290",
                                    "name": "김현수",
                                    "pos": "좌",
                                    "ab": 4,
                                    "hit": 2,
                                    "rbi": 1,
                                    "run": 1,
                                    "bb": 0,
                                    "kk": 1,
                                    "hr": 1,
                                    "sb": 0,
                                    "inn1": "좌안",
                                    "inn3": "우홈"
                                  }
                                ],
                                "home": []
                              },
                              "pitchersBoxscore": {
                                "away": [],
                                "home": [
                                  {
                                    "pcode": "63492",
                                    "name": "김광현",
                                    "inn": "6",
                                    "hit": 3,
                                    "r": 1,
                                    "er": 1,
                                    "bb": 1,
                                    "bbhp": 1,
                                    "kk": 5,
                                    "hr": 0,
                                    "bf": 90,
                                    "wls": "승"
                                  }
                                ]
                              },
                              "etcRecords": [
                                {
                                  "how": "홈런",
                                  "result": "김현수(3회1점)"
                                }
                              ]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        KboGameRecord record = fetcher.fetch("20260606KTSK02026");

        // then
        // record API의 gameInfo에는 gameId가 없으므로 요청 gameId가 결과 모델에 보존되어야 한다.
        assertThat(record.getGameId()).isEqualTo("20260606KTSK02026");
        assertThat(record.getBatterRecords()).singleElement()
                .satisfies(batter -> {
                    assertThat(batter.getPlayerCode()).isEqualTo("76290");
                    assertThat(batter.getTeamCode()).isEqualTo("KT");
                    assertThat(batter.getPosition()).isEqualTo(PlayerPosition.LF);
                    assertThat(batter.getPaResults()).containsExactly("좌안", "우홈");
                });
        assertThat(record.getPitcherRecords()).singleElement()
                .satisfies(pitcher -> {
                    assertThat(pitcher.getPlayerCode()).isEqualTo("63492");
                    assertThat(pitcher.getTeamCode()).isEqualTo("SK");
                    assertThat(pitcher.getInn()).isEqualTo("6");
                });
        assertThat(record.getEtcRecords()).singleElement()
                .satisfies(etc -> assertThat(etc.getHow()).isEqualTo("홈런"));
        server.verify();
    }

    @Test
    @EnabledIfSystemProperty(named = "external.kbo.live-test", matches = "true")
    void fetch는_실제_SSHT_record_API에서_성영탁_투수명을_매핑한다() {
        // 외부 API 스펙 확인용 테스트다. 평소 테스트에서는 제외하고, 필요할 때만 system property로 명시 실행한다.
        KboGameRecordApiFetcher fetcher = new KboGameRecordApiFetcher(
                RestClient.builder()
                        .baseUrl("https://api-gw.sports.naver.com")
                        .build(),
                new KboGameStatusMapper());

        KboGameRecord record = fetcher.fetch("20260606SSHT02026");

        assertThat(record.getGameId()).isEqualTo("20260606SSHT02026");
        assertThat(record.getPitcherRecords())
                .filteredOn(pitcher -> "54610".equals(pitcher.getPlayerCode()))
                .singleElement()
                .satisfies(pitcher -> {
                    assertThat(pitcher.getName()).isEqualTo("성영탁");
                    assertThat(pitcher.getTeamCode()).isEqualTo("HT");
                });
    }

}
