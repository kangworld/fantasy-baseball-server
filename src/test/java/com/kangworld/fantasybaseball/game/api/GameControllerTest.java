package com.kangworld.fantasybaseball.game.api;

import static com.kangworld.fantasybaseball.support.AuthTestCookies.accessTokenCookie;
import static com.kangworld.fantasybaseball.support.AuthTestCookies.trustedOrigin;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import com.kangworld.fantasybaseball.external.kbo.model.KboLiveGameState;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.model.KboTeamRank;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboLiveGameStateFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboTeamRankFetchService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameControllerTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 28);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenFactory jwtAccessTokenFactory;

    @MockitoBean
    private KboScheduleFetchService scheduleFetchService;

    @MockitoBean
    private KboGameRecordFetchService gameRecordFetchService;

    @MockitoBean
    private KboLiveGameStateFetchService liveGameStateFetchService;

    @MockitoBean
    private KboTeamRankFetchService teamRankFetchService;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUpClock() {
        given(clock.instant()).willReturn(Instant.parse("2026-04-28T03:00:00Z"));
        given(clock.getZone()).willReturn(SEOUL_ZONE_ID);
    }

    @Test
    void schedule은_accessToken이_없으면_401을_반환한다() throws Exception {
        // given

        // when then
        mockMvc.perform(get("/api/games/schedule"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void schedule은_날짜로_경기일정을_조회하고_상태별로_필터링하지_않는다() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 4, 28);
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        doReturn(List.of(
                buildScheduleGame("game-1", KboGameStatus.READY),
                buildScheduleGame("game-2", KboGameStatus.PLAYING)
        )).when(scheduleFetchService).fetch(date);

        // when then
        mockMvc.perform(get("/api/games/schedule")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .param("date", "2026-04-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].gameId").value("game-1"))
                .andExpect(jsonPath("$.data[0].status").value("READY"))
                .andExpect(jsonPath("$.data[1].gameId").value("game-2"))
                .andExpect(jsonPath("$.data[1].status").value("PLAYING"))
                .andExpect(jsonPath("$.data[1].statusCode").value("STARTED"))
                .andExpect(jsonPath("$.data[1].statusInfo").value("3회말"))
                .andExpect(jsonPath("$.data[0].homeTeamCode").value("KT"))
                .andExpect(jsonPath("$.data[0].awayTeamCode").value("LG"));
    }

    @Test
    void liveStates는_오늘_전체_경기의_실시간_상태를_반환한다() throws Exception {
        // givenb
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        doReturn(List.of(
                buildScheduleGame("game-1", KboGameStatus.READY),
                buildScheduleGame("game-2", KboGameStatus.PLAYING)
        )).when(scheduleFetchService).fetch(TODAY);
        doReturn(buildReadyLiveState("game-1")).when(liveGameStateFetchService).fetch("game-1");
        doReturn(buildLiveState("game-2")).when(liveGameStateFetchService).fetch("game-2");

        // when then
        mockMvc.perform(get("/api/games/live-states")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].gameId").value("game-1"))
                .andExpect(jsonPath("$.data[0].statusInfo").value("경기전"))
                .andExpect(jsonPath("$.data[0].statusCode").value("READY"))
                .andExpect(jsonPath("$.data[0].inning").doesNotExist())
                .andExpect(jsonPath("$.data[0].topInning").doesNotExist())
                .andExpect(jsonPath("$.data[1].gameId").value("game-2"))
                .andExpect(jsonPath("$.data[1].status").value("PLAYING"))
                .andExpect(jsonPath("$.data[1].statusCode").value("STARTED"))
                .andExpect(jsonPath("$.data[1].inning").value(3))
                .andExpect(jsonPath("$.data[1].topInning").value(false))
                .andExpect(jsonPath("$.data[1].homeTeamScore").value(3))
                .andExpect(jsonPath("$.data[1].awayTeamScore").value(4));
    }

    @Test
    void liveState는_단일_경기의_실시간_상태를_반환한다() throws Exception {
        // given
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        doReturn(buildLiveState("game-2")).when(liveGameStateFetchService).fetch("game-2");

        // when then
        mockMvc.perform(get("/api/games/game-2/live-state")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.gameId").value("game-2"))
                .andExpect(jsonPath("$.data.status").value("PLAYING"))
                .andExpect(jsonPath("$.data.statusCode").value("STARTED"))
                .andExpect(jsonPath("$.data.statusInfo").value("3회말"))
                .andExpect(jsonPath("$.data.inning").value(3))
                .andExpect(jsonPath("$.data.topInning").value(false));
    }

    @Test
    void teamRanks는_시즌으로_KBO_팀_순위표를_조회한다() throws Exception {
        // given
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        doReturn(List.of(buildTeamRank())).when(teamRankFetchService).fetch(2026);

        // when then
        mockMvc.perform(get("/api/games/team-ranks")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .param("season", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].ranking").value(1))
                .andExpect(jsonPath("$.data[0].teamCode").value("KT"))
                .andExpect(jsonPath("$.data[0].teamName").value("KT"))
                .andExpect(jsonPath("$.data[0].winningRate").value(0.68))
                .andExpect(jsonPath("$.data[0].gameBehind").value(0.0))
                .andExpect(jsonPath("$.data[0].continuousGameResult").value("1승"))
                .andExpect(jsonPath("$.data[0].nextScheduleGameId").value("20260428LGKT02026"))
                .andExpect(jsonPath("$.data[0].opposingTeamName").value("LG"));
    }

    @Test
    void teamRanks는_시즌이_없으면_판타지_기준일의_연도로_조회한다() throws Exception {
        // given
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        doReturn(List.of(buildTeamRank())).when(teamRankFetchService).fetch(2026);

        // when then
        mockMvc.perform(get("/api/games/team-ranks")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamCode").value("KT"));
    }

    @Test
    void teamRanks는_accessToken이_없으면_401을_반환한다() throws Exception {
        // given

        // when then
        mockMvc.perform(get("/api/games/team-ranks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void playerRecords는_당일_경기기록을_타자와_투수_단일_배열로_반환한다() throws Exception {
        // given
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        doReturn(List.of(
                buildScheduleGame("game-1", KboGameStatus.READY),
                buildScheduleGame("game-2", KboGameStatus.PLAYING)
        )).when(scheduleFetchService).fetch(TODAY);
        doReturn(buildGameRecord()).when(gameRecordFetchService).fetch("game-2");

        // when then
        mockMvc.perform(get("/api/player-records")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin()))
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
                .statusCode(status == KboGameStatus.PLAYING ? "STARTED" : "READY")
                .statusInfo(status == KboGameStatus.PLAYING ? "3회말" : "경기전")
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

    private KboTeamRank buildTeamRank() {
        return KboTeamRank.builder()
                .ranking(1)
                .teamCode("KT")
                .teamName("KT")
                .teamShortName("KT")
                .gameCount(25)
                .winGameCount(17)
                .drawnGameCount(0)
                .loseGameCount(8)
                .winningRate(0.68)
                .gameBehind(0.0)
                .continuousGameResult("1승")
                .lastFiveGames("WWLLW")
                .nextScheduleGameId("20260428LGKT02026")
                .opposingTeamName("LG")
                .build();
    }

    private KboLiveGameState buildLiveState(String gameId) {
        return buildLiveState(gameId, KboGameStatus.PLAYING, "STARTED", "3회말", "3회말");
    }

    private KboLiveGameState buildReadyLiveState(String gameId) {
        return buildLiveState(gameId, KboGameStatus.READY, "READY", "경기전", "1회초");
    }

    private KboLiveGameState buildLiveState(
            String gameId,
            KboGameStatus status,
            String statusCode,
            String statusInfo,
            String currentInning) {
        return KboLiveGameState.builder()
                .gameId(gameId)
                .status(status)
                .statusCode(statusCode)
                .statusInfo(statusInfo)
                .currentInning(currentInning)
                .homeTeamScore(3)
                .awayTeamScore(4)
                .homeTeamCode("KT")
                .awayTeamCode("LG")
                .build();
    }

}
