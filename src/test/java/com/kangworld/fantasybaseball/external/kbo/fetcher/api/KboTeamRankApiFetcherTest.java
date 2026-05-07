package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.kangworld.fantasybaseball.external.kbo.model.KboTeamRank;

class KboTeamRankApiFetcherTest {

    @Test
    void fetch는_시즌과_정규시즌_queryParam으로_팀_순위를_조회한다() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboTeamRankApiFetcher fetcher = new KboTeamRankApiFetcher(builder.build());

        server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/statistics/categories/kbo/seasons/2026/teams");
            assertThat(request.getURI().getQuery()).contains("gameType=REGULAR_SEASON");
        }).andRespond(withSuccess("""
                {
                  "result": {
                    "seasonTeamStats": [
                      {
                        "teamId": "KT",
                        "teamName": "KT",
                        "teamShortName": "KT",
                        "ranking": 1,
                        "wra": 0.68,
                        "gameCount": 25,
                        "winGameCount": 17,
                        "drawnGameCount": 0,
                        "loseGameCount": 8,
                        "gameBehind": 0.0,
                        "continuousGameResult": "1승",
                        "lastFiveGames": "WWLLW",
                        "nextScheduleGameId": "20260428LGKT02026",
                        "opposingTeamName": "LG"
                      }
                    ]
                  }
                }
                """, MediaType.APPLICATION_JSON));

        // when
        List<KboTeamRank> ranks = fetcher.fetch(2026);

        // then
        assertThat(ranks).hasSize(1);
        assertThat(ranks.get(0).getRanking()).isEqualTo(1);
        assertThat(ranks.get(0).getTeamCode()).isEqualTo("KT");
        assertThat(ranks.get(0).getWinningRate()).isEqualTo(0.68);
        assertThat(ranks.get(0).getContinuousGameResult()).isEqualTo("1승");
        assertThat(ranks.get(0).getNextScheduleGameId()).isEqualTo("20260428LGKT02026");
        server.verify();
    }

    @Test
    void fetch는_응답_목록이_없으면_빈_배열을_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboTeamRankApiFetcher fetcher = new KboTeamRankApiFetcher(builder.build());

        server.expect(request -> {
        }).andRespond(withSuccess("""
                {
                  "result": {
                  }
                }
                """, MediaType.APPLICATION_JSON));

        // when
        List<KboTeamRank> ranks = fetcher.fetch(2026);

        // then
        assertThat(ranks).isEmpty();
        server.verify();
    }

}
