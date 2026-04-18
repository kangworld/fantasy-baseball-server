package com.kangworld.fantasybaseball.game.api;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kangworld.fantasybaseball.auth.token.JwtAccessTokenFactory;
import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenFactory jwtAccessTokenFactory;

    @MockitoBean
    private KboScheduleFetchService scheduleFetchService;

    @MockitoBean
    private KboGameRecordFetchService gameRecordFetchService;

    @Test
    void schedule은_accessToken이_없으면_401을_반환한다() throws Exception {
        // given

        // when then
        mockMvc.perform(get("/api/games/schedule"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void schedule은_날짜와_상태로_경기일정을_조회한다() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 4, 28);
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        doReturn(List.of(
                buildScheduleGame("game-1", KboGameStatus.READY),
                buildScheduleGame("game-2", KboGameStatus.PLAYING)
        )).when(scheduleFetchService).fetch(date);

        // when then
        mockMvc.perform(get("/api/games/schedule")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("date", "2026-04-28")
                        .param("status", "PLAYING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].gameId").value("game-2"))
                .andExpect(jsonPath("$.data[0].status").value("PLAYING"))
                .andExpect(jsonPath("$.data[0].statusCode").value("STARTED"))
                .andExpect(jsonPath("$.data[0].statusInfo").value("경기중"))
                .andExpect(jsonPath("$.data[0].homeTeamCode").value("KT"))
                .andExpect(jsonPath("$.data[0].awayTeamCode").value("LG"));
    }

    @Test
    void playerRecords는_당일_경기기록을_타자와_투수_단일_배열로_반환한다() throws Exception {
        // given
        LocalDate today = LocalDate.now();
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        doReturn(List.of(buildScheduleGame("game-2", KboGameStatus.PLAYING)))
                .when(scheduleFetchService).findRecordTargetGames(today);
        doReturn(buildGameRecord()).when(gameRecordFetchService).fetch("game-2");

        // when then
        mockMvc.perform(get("/api/player-records")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].gameId").value("game-2"))
                .andExpect(jsonPath("$.data[0].playerId").value(78548))
                .andExpect(jsonPath("$.data[0].playerType").value("BATTER"))
                .andExpect(jsonPath("$.data[0].pos").value("C"))
                .andExpect(jsonPath("$.data[0].ab").value(4))
                .andExpect(jsonPath("$.data[0].rbi").value(5))
                .andExpect(jsonPath("$.data[1].playerId").value(63492))
                .andExpect(jsonPath("$.data[1].playerType").value("PITCHER"))
                .andExpect(jsonPath("$.data[1].pos").value("P"))
                .andExpect(jsonPath("$.data[1].inn").value("6"))
                .andExpect(jsonPath("$.data[1].er").value(1));
    }

    private KboScheduleGame buildScheduleGame(String gameId, KboGameStatus status) {
        return KboScheduleGame.builder()
                .gameId(gameId)
                .startAt(LocalDateTime.of(2026, 4, 28, 18, 30))
                .status(status)
                .statusCode(status == KboGameStatus.PLAYING ? "STARTED" : "BEFORE")
                .statusInfo(status == KboGameStatus.PLAYING ? "경기중" : "경기전")
                .homeTeamCode("KT")
                .homeTeamName("KT")
                .awayTeamCode("LG")
                .awayTeamName("LG")
                .build();
    }

    private KboGameRecord buildGameRecord() {
        return KboGameRecord.builder()
                .gameId("game-2")
                .status(KboGameStatus.PLAYING)
                .batterRecords(List.of(KboBatterRecord.builder()
                        .playerCode("78548")
                        .name("장성우")
                        .pos("C")
                        .ab(4)
                        .hit(3)
                        .rbi(5)
                        .run(3)
                        .bb(1)
                        .kk(0)
                        .hr(1)
                        .sb(0)
                        .build()))
                .pitcherRecords(List.of(KboPitcherRecord.builder()
                        .playerCode("63492")
                        .name("김광현")
                        .inn("6")
                        .hit(4)
                        .r(2)
                        .er(1)
                        .bb(1)
                        .kk(7)
                        .hr(0)
                        .bf(24)
                        .build()))
                .build();
    }

}
