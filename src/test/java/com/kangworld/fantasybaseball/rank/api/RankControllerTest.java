package com.kangworld.fantasybaseball.rank.api;

import static com.kangworld.fantasybaseball.support.AuthTestCookies.accessTokenCookie;
import static com.kangworld.fantasybaseball.support.AuthTestCookies.trustedOrigin;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kangworld.fantasybaseball.auth.token.JwtAccessTokenFactory;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.rank.service.RankService;
import com.kangworld.fantasybaseball.rank.service.dto.DailyRankItemResponse;
import com.kangworld.fantasybaseball.rank.service.dto.DailyRankPageResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ResultStatus;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardBreakdownResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardDailyResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardSeasonResponse;
import com.kangworld.fantasybaseball.rank.service.dto.SeasonRankItemResponse;
import com.kangworld.fantasybaseball.rank.service.dto.SeasonRankPageResponse;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenFactory jwtAccessTokenFactory;

    @MockitoBean
    private RankService rankService;

    @Test
    void scoreboard_me는_accessToken이_없으면_401을_반환한다() throws Exception {
        // given

        // when then
        mockMvc.perform(get("/api/scoreboard/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void scoreboard_me는_내_점수판을_반환한다() throws Exception {
        // given
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        LocalDate date = LocalDate.of(2026, 5, 11);
        given(rankService.findMyScoreboard(eq(1L), eq(date))).willReturn(scoreboard(date));

        // when then
        mockMvc.perform(get("/api/scoreboard/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .param("date", "2026-05-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-05-11"))
                .andExpect(jsonPath("$.data.status").value("FINALIZED"))
                .andExpect(jsonPath("$.data.daily.point").value(120))
                .andExpect(jsonPath("$.data.daily.displayRankNo").value(2))
                .andExpect(jsonPath("$.data.daily.previousRankNo").value(6))
                .andExpect(jsonPath("$.data.season.point").value(1020))
                .andExpect(jsonPath("$.data.season.previousRankNo").value(7))
                .andExpect(jsonPath("$.data.scoreBreakdown.gameDate").value("2026-05-10"))
                .andExpect(jsonPath("$.data.scoreBreakdown.players[0].games[0].point").value(120));
    }

    @Test
    void ranks_daily는_accessToken_없이_일간_랭킹을_조회한다() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 5, 11);
        given(rankService.findDailyRanks(eq(date), eq(1), eq(20))).willReturn(dailyRanks(date));

        // when then
        mockMvc.perform(get("/api/ranks/daily")
                        .param("date", "2026-05-11")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.status").value("FINALIZED"))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.ranks[0].memberId").value(10))
                .andExpect(jsonPath("$.data.ranks[0].displayName").value("강민구"))
                .andExpect(jsonPath("$.data.ranks[0].previousRankNo").value(4))
                .andExpect(jsonPath("$.data.ranks[0].point").value(150));
    }

    @Test
    void ranks_season은_accessToken_없이_시즌_랭킹을_조회한다() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 5, 11);
        given(rankService.findSeasonRanks(eq(date), eq(0), eq(50))).willReturn(seasonRanks(date));

        // when then
        mockMvc.perform(get("/api/ranks/season")
                        .param("date", "2026-05-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.status").value("FINALIZED"))
                .andExpect(jsonPath("$.data.size").value(50))
                .andExpect(jsonPath("$.data.ranks[0].displayName").value("강민구"))
                .andExpect(jsonPath("$.data.ranks[0].previousRankNo").value(2))
                .andExpect(jsonPath("$.data.ranks[0].dailyPoint").value(80));
    }

    @Test
    void ranks_daily는_size가_100을_초과하면_400을_반환한다() throws Exception {
        // given

        // when then
        mockMvc.perform(get("/api/ranks/daily")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void ranks_daily는_page가_음수이면_400을_반환한다() throws Exception {
        // given

        // when then
        mockMvc.perform(get("/api/ranks/daily")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void ranks_season은_size가_1보다_작으면_400을_반환한다() throws Exception {
        // given

        // when then
        mockMvc.perform(get("/api/ranks/season")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private ScoreboardResponse scoreboard(LocalDate date) {
        return ScoreboardResponse.builder()
                .date(date)
                .status(ResultStatus.FINALIZED)
                .scoreBreakdown(new ScoreboardBreakdownResponse(date.minusDays(1), 120, 120, List.of(
                        new ScoreboardBreakdownResponse.Player(10, PlayerPosition.P, 1, 120, List.of(
                                new ScoreboardBreakdownResponse.Game("20260510LGHH0", 120))))))
                .daily(ScoreboardDailyResponse.builder()
                        .point(120)
                        .rankNo(3)
                        .displayRankNo(2)
                        .previousRankNo(6)
                        .build())
                .season(ScoreboardSeasonResponse.builder()
                        .point(1020)
                        .dailyPoint(120)
                        .rankNo(5)
                        .displayRankNo(4)
                        .previousRankNo(7)
                        .build())
                .build();
    }

    private DailyRankPageResponse dailyRanks(LocalDate date) {
        return DailyRankPageResponse.builder()
                .date(date)
                .status(ResultStatus.FINALIZED)
                .page(1)
                .size(20)
                .ranks(List.of(DailyRankItemResponse.builder()
                        .rankNo(1)
                        .displayRankNo(1)
                        .previousRankNo(4)
                        .memberId(10L)
                        .displayName("강민구")
                        .point(150)
                        .build()))
                .build();
    }

    private SeasonRankPageResponse seasonRanks(LocalDate date) {
        return SeasonRankPageResponse.builder()
                .date(date)
                .status(ResultStatus.FINALIZED)
                .page(0)
                .size(50)
                .ranks(List.of(SeasonRankItemResponse.builder()
                        .rankNo(1)
                        .displayRankNo(1)
                        .previousRankNo(2)
                        .memberId(10L)
                        .displayName("강민구")
                        .point(1200)
                        .dailyPoint(80)
                        .build()))
                .build();
    }

}
