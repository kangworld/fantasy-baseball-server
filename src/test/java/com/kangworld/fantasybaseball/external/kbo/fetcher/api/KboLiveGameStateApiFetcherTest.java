package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.external.kbo.model.KboLiveGameState;

class KboLiveGameStateApiFetcherTest {

    @Test
    void fetch는_polling_상태값을_보존하고_BEFORE를_READY로_매핑한다() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboLiveGameStateApiFetcher fetcher = new KboLiveGameStateApiFetcher(builder.build(), new KboGameStatusMapper());

        server.expect(requestTo("https://example.test/schedule/games/20260512HHWO02026/game-polling"))
                .andRespond(withSuccess("""
                        {
                          "result": {
                            "game": {
                              "gameId": "20260512HHWO02026",
                              "statusCode": "BEFORE",
                              "statusInfo": "경기전",
                              "currentInning": null,
                              "homeTeamScore": 0,
                              "awayTeamScore": 0,
                              "homeTeamCode": "WO",
                              "awayTeamCode": "HH",
                              "cancel": false,
                              "suspended": false
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        KboLiveGameState state = fetcher.fetch("20260512HHWO02026");

        // then
        assertThat(state.getStatus()).isEqualTo(KboGameStatus.READY);
        assertThat(state.getStatusCode()).isEqualTo("BEFORE");
        assertThat(state.getStatusInfo()).isEqualTo("경기전");
        server.verify();
    }

}
