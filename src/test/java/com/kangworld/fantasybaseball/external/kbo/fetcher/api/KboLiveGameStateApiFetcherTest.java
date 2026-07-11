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
    void fetch는_대타와_대주자_중계가_포함된_실제_polling_응답에서_경기상태를_변환한다() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboLiveGameStateApiFetcher fetcher = new KboLiveGameStateApiFetcher(
                builder.build(),
                new KboGameStatusMapper()
        );

        String response = KboResponseFixtures.json("game-polling-20260426KTSK02026.json");
        assertThat(response).contains("\"posName\": \"대타\"", "\"posName\": \"대주자\"");
        server.expect(requestTo("https://example.test/schedule/games/20260426KTSK02026/game-polling"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        KboLiveGameState state = fetcher.fetch("20260426KTSK02026");

        assertThat(state.getGameId()).isEqualTo("20260426KTSK02026");
        assertThat(state.getStatus()).isEqualTo(KboGameStatus.FINISHED);
        assertThat(state.getStatusCode()).isEqualTo("RESULT");
        assertThat(state.getStatusInfo()).isEqualTo("9회말");
        assertThat(state.getCurrentInning()).isEqualTo("9회말");
        assertThat(state.getAwayTeamScore()).isEqualTo(12);
        assertThat(state.getHomeTeamScore()).isEqualTo(2);
        server.verify();
    }

}
