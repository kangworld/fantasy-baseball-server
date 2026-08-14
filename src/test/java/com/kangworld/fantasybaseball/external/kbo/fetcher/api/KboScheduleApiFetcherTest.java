package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatusMapper;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;

class KboScheduleApiFetcherTest {

    /** 빈 일정 목록은 휴식일로 본다. 응답에 일정 목록이 아예 없으면 정상 휴식일로 처리할 수 없다. */
    @Test
    void fetch는_빈_일정과_누락된_일정을_구분한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboScheduleApiFetcher fetcher = new KboScheduleApiFetcher(builder.build(), new KboGameStatusMapper());
        LocalDate date = LocalDate.of(2026, 5, 18);
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/schedule/games"))
                .andRespond(withSuccess("{\"result\":{\"games\":[]}}", MediaType.APPLICATION_JSON));
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/schedule/games"))
                .andRespond(withSuccess("{\"result\":{}}", MediaType.APPLICATION_JSON));

        assertThat(fetcher.fetch(date, date)).isEmpty();
        assertThatThrownBy(() -> fetcher.fetch(date, date)).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void fetch는_실제_일정_응답에서_더블헤더_두_경기의_gameId와_시작시각을_보존한다() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KboScheduleApiFetcher fetcher = new KboScheduleApiFetcher(builder.build(), new KboGameStatusMapper());

        server.expect(request -> {
            String query = request.getURI().getQuery();
            assertThat(query).contains("fromDate=2024-04-21");
            assertThat(query).contains("toDate=2024-04-21");
        }).andRespond(withSuccess(
                KboResponseFixtures.json("schedule-20240421-doubleheader.json"),
                MediaType.APPLICATION_JSON
        ));

        List<KboScheduleGame> games = fetcher.fetch(
                LocalDate.of(2024, 4, 21),
                LocalDate.of(2024, 4, 21)
        );

        assertThat(games).hasSize(3).allSatisfy(game ->
                assertThat(game.getStatus()).isEqualTo(KboGameStatus.FINISHED));
        assertThat(games)
                .filteredOn(game -> "KT".equals(game.getAwayTeamCode()) && "LT".equals(game.getHomeTeamCode()))
                .extracting(KboScheduleGame::getGameId)
                .containsExactly("20240421KTLT12024", "20240421KTLT22024");
        assertThat(games)
                .filteredOn(game -> "KT".equals(game.getAwayTeamCode()) && "LT".equals(game.getHomeTeamCode()))
                .extracting(KboScheduleGame::getStartAt)
                .containsExactly(
                        LocalDateTime.of(2024, 4, 21, 14, 0),
                        LocalDateTime.of(2024, 4, 21, 17, 49)
                );
        server.verify();
    }

}
