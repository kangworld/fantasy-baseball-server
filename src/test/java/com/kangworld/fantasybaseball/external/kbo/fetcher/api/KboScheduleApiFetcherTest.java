package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;

class KboScheduleApiFetcherTest {

    @Test
    void fetch는_날짜_queryParam을_yyyy_MM_dd_형식으로_전달한다() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboScheduleApiFetcher fetcher = new KboScheduleApiFetcher(builder.build(), new KboGameStatusMapper());

        server.expect(request -> {
            String query = request.getURI().getQuery();
            assertThat(query).contains("fromDate=2025-10-12");
            assertThat(query).contains("toDate=2025-10-12");
        }).andRespond(withSuccess("""
                {
                  "result": {
                    "games": [
                      {
                        "gameId": "20260512HHWO02026",
                        "categoryId": "kbo",
                        "gameDateTime": "2026-05-12T18:30:00",
                        "homeTeamCode": "WO",
                        "homeTeamName": "키움",
                        "awayTeamCode": "HH",
                        "awayTeamName": "한화",
                        "statusCode": "READY",
                        "statusInfo": "경기전",
                        "cancel": false,
                        "suspended": false
                      }
                    ]
                  }
                }
                """, MediaType.APPLICATION_JSON));

        // when
        List<KboScheduleGame> games = fetcher.fetch(LocalDate.of(2025, 10, 12), LocalDate.of(2025, 10, 12));

        // then
        assertThat(games).hasSize(1);
        assertThat(games.get(0).getStatus()).isEqualTo(KboGameStatus.READY);
        assertThat(games.get(0).getStatusCode()).isEqualTo("READY");
        assertThat(games.get(0).getStatusInfo()).isEqualTo("경기전");
        server.verify();
    }

}
