package com.kangworld.fantasybaseball.roster.api;

import static com.kangworld.fantasybaseball.support.AuthTestCookies.accessTokenCookie;
import static com.kangworld.fantasybaseball.support.AuthTestCookies.trustedOrigin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangworld.fantasybaseball.auth.token.JwtAccessTokenFactory;
import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.game.service.GameService;
import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.rediscore.lock.RedisLock;
import com.kangworld.rediscore.lock.RedisLockManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
        "delete from roster_player",
        "delete from roster",
        "delete from player_profile",
        "delete from player"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RosterControllerTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalDate ROSTER_DATE = LocalDate.of(2026, 5, 4);
    private static final LocalDateTime ROSTER_NOW = LocalDateTime.of(2026, 5, 4, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtAccessTokenFactory jwtAccessTokenFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RedisLockManager redisLockManager;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private KboScheduleFetchService scheduleFetchService;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2026-05-04T03:00:00Z"));
        given(clock.getZone()).willReturn(SEOUL_ZONE_ID);
        given(redisLockManager.acquire(eq("roster"), eq(3L), any()))
                .willReturn(new RedisLock("fantasy-baseball::roster-1", "token"));
        given(scheduleFetchService.fetchFresh(ROSTER_DATE)).willReturn(List.of());
    }

    @Test
    void rosters_me는_accessToken이_없으면_401을_반환한다() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rosters_me는_허용되지_않은_referer이면_403을_반환한다() throws Exception {
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken))
                        .header(HttpHeaders.REFERER, "https://evil.example.com/rosters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("허용되지 않은 요청 출처입니다."));
    }

    @Test
    void rosters_config는_라인업_보드_구성에_필요한_로스터_규칙을_반환한다() throws Exception {
        // when then
        mockMvc.perform(get("/api/rosters/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slotCount").doesNotExist())
                .andExpect(jsonPath("$.data.costLimit").value(40))
                .andExpect(jsonPath("$.data.positions.length()").value(10))
                .andExpect(jsonPath("$.data.positions[0].rosterPosition").value("P"))
                .andExpect(jsonPath("$.data.positions[0].count").value(5))
                .andExpect(jsonPath("$.data.positions[1].rosterPosition").value("C"))
                .andExpect(jsonPath("$.data.positions[9].rosterPosition").value("DH"));
    }

    @Test
    void rosters_me는_로스터를_제출한다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("로스터 제출이 완료되었습니다."))
                .andExpect(jsonPath("$.data.date").value(ROSTER_DATE.toString()))
                .andExpect(jsonPath("$.data.players.length()").value(14))
                .andExpect(jsonPath("$.data.players[0].rosterPosition").value("P"))
                .andExpect(jsonPath("$.data.players[0].positionOrder").value(1))
                .andExpect(jsonPath("$.data.players[0].playerId").value(101));
    }

    @Test
    void rosters_me는_제출한_로스터를_조회한다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));
        mockMvc.perform(post("/api/rosters/me")
                .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // when then
        mockMvc.perform(get("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .param("date", ROSTER_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value(ROSTER_DATE.toString()))
                .andExpect(jsonPath("$.data.players.length()").value(14));
    }

    @Test
    void rosters_me는_로스터가_없으면_data_null을_반환한다() throws Exception {
        // given
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");

        // when then
        mockMvc.perform(get("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .param("date", ROSTER_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rosters_me_playerRecords는_제출_로스터_선수들의_기록을_반환한다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));
        mockMvc.perform(post("/api/rosters/me")
                .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
        given(gameService.findTodayPlayerRecordsByPlayerIds(argThat(playerIds ->
                playerIds.size() == 14 && playerIds.contains(101L) && playerIds.contains(114L))))
                .willReturn(List.of(buildPlayerRecord(101L)));

        // when then
        mockMvc.perform(get("/api/rosters/me/player-records")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].playerId").value(101))
                .andExpect(jsonPath("$.data[0].playerType").value("BATTER"));
    }

    @Test
    void rosters_me_playerRecords는_제출_로스터가_없으면_빈_배열을_반환한다() throws Exception {
        // given
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");

        // when then
        mockMvc.perform(get("/api/rosters/me/player-records")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));

        verifyNoInteractions(gameService);
    }

    @Test
    void rosters_me는_기존_로스터를_전체_교체한다() throws Exception {
        // given
        List<Long> firstPlayerIds = defaultPlayerIds();
        List<Long> secondPlayerIds = replacementPlayerIds();
        insertRosterPlayers(firstPlayerIds);
        insertRosterPlayers(secondPlayerIds);

        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        mockMvc.perform(post("/api/rosters/me")
                .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rosterRequest(firstPlayerIds))));

        // when then
        mockMvc.perform(put("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rosterRequest(secondPlayerIds))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로스터 수정이 완료되었습니다."))
                .andExpect(jsonPath("$.data.players[0].playerId").value(201));
    }

    @Test
    void rosters_me는_중복_선수를_거절한다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        List<Long> playerIds = defaultPlayerIds();
        playerIds.set(13, playerIds.get(0));

        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(playerIds));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("같은 선수는 중복 등록할 수 없습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rosters_me는_비용_제한을_초과하면_거절한다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        jdbcTemplate.update("update player_profile set cost = 3");

        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("로스터 비용 제한을 초과했습니다."));
    }

    @Test
    void rosters_me는_라인업_칸에_맞지_않는_선수를_거절한다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        List<Long> playerIds = defaultPlayerIds();
        playerIds.set(0, 106L);
        playerIds.set(5, 101L);

        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(playerIds));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("로스터 포지션에 등록할 수 없는 선수 유형입니다."));
    }

    /** 일정 조회에 실패하면 로스터 제출을 거절하고, 로스터가 저장되지 않아야 한다. */
    @Test
    void rosters_me는_일정_조회에_실패하면_제출을_저장하지_않는다() throws Exception {
        insertRosterPlayers(defaultPlayerIds());
        given(scheduleFetchService.fetchFresh(ROSTER_DATE)).willThrow(new IllegalStateException("일정 누락"));
        String token = jwtAccessTokenFactory.create(1L, "USER");
        String body = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(token)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/rosters/me")
                        .cookie(accessTokenCookie(token)).param("date", ROSTER_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /** 더블헤더 첫 경기가 예정보다 일찍 시작하면 해당 팀 선수를 추가할 수 없다. */
    @Test
    void rosters_me는_더블헤더_첫_경기가_예정보다_먼저_시작하면_제출을_막는다() throws Exception {
        insertRosterPlayers(defaultPlayerIds());
        given(scheduleFetchService.fetchFresh(ROSTER_DATE)).willReturn(List.of(
                scheduleGame("game-1", ROSTER_NOW.plusMinutes(10), "LG", "HH", KboGameStatus.PLAYING),
                scheduleGame("game-2", ROSTER_NOW.plusHours(3), "LG", "HH", KboGameStatus.READY)
        ));
        String token = jwtAccessTokenFactory.create(1L, "USER");

        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(token)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()))))
                .andExpect(status().isBadRequest());
    }

    /** 로스터 제출은 예정 시각이 바뀐 최신 일정을 기준으로 판단한다. */
    @Test
    void rosters_me는_갱신된_시작_시각을_기준으로_제출을_판단한다() throws Exception {
        insertRosterPlayers(defaultPlayerIds());
        given(scheduleFetchService.fetch(ROSTER_DATE)).willReturn(List.of(
                scheduleGame("game-1", ROSTER_NOW.plusHours(2), "LG", "HH", KboGameStatus.READY)
        ));
        given(scheduleFetchService.fetchFresh(ROSTER_DATE)).willReturn(List.of(
                scheduleGame("game-1", ROSTER_NOW, "LG", "HH", KboGameStatus.READY)
        ));
        String token = jwtAccessTokenFactory.create(1L, "USER");

        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(token)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rosters_me는_이미_시작한_경기의_선수를_제출할_수_없다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        givenStartedSchedule("LG", "HH");
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rosters_me는_이미_시작한_경기의_선수가_그대로_남아있으면_수정한다() throws Exception {
        // given
        List<Long> firstPlayerIds = defaultPlayerIds();
        List<Long> secondPlayerIds = replacementPlayerIds();
        secondPlayerIds.set(0, firstPlayerIds.get(0));

        insertRosterPlayers(firstPlayerIds);
        insertRosterPlayers(replacementPlayerIds());

        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        mockMvc.perform(post("/api/rosters/me")
                .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rosterRequest(firstPlayerIds))));

        givenStartedSchedule("LG", "HH");

        // when then
        mockMvc.perform(put("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rosterRequest(secondPlayerIds))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.players[0].playerId").value(101))
                .andExpect(jsonPath("$.data.players[1].playerId").value(202));
    }

    @Test
    void rosters_me는_이미_시작한_경기의_선수를_교체하면_거절한다() throws Exception {
        // given
        List<Long> playerIds = defaultPlayerIds();
        List<Long> movedPlayerIds = defaultPlayerIds();
        movedPlayerIds.set(0, replacementPlayerIds().get(0));

        insertRosterPlayers(playerIds);
        insertRosterPlayers(replacementPlayerIds());

        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        mockMvc.perform(post("/api/rosters/me")
                .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rosterRequest(playerIds))));

        givenStartedSchedule("LG", "HH");

        // when then
        mockMvc.perform(put("/api/rosters/me")
                        .cookie(accessTokenCookie(accessToken)).with(trustedOrigin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rosterRequest(movedPlayerIds))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private Map<String, Object> rosterRequest(List<Long> playerIds) {
        List<Map<String, Object>> players = IntStream.range(0, playerIds.size())
                .mapToObj(index -> Map.<String, Object>of(
                        "rosterPosition", rosterPosition(index).name(),
                        "positionOrder", positionOrder(index),
                        "playerId", playerIds.get(index)
                ))
                .toList();

        return Map.of(
                "players", players
        );
    }

    private void insertRosterPlayers(List<Long> playerIds) {
        String[] positions = {"P", "P", "P", "P", "P", "C", "B1", "B2", "B3", "SS", "LF", "CF", "RF", "DH"};
        String[] types = {
                "PITCHER", "PITCHER", "PITCHER", "PITCHER", "PITCHER",
                "BATTER", "BATTER", "BATTER", "BATTER", "BATTER", "BATTER", "BATTER", "BATTER", "BATTER"
        };
        LocalDateTime now = ROSTER_NOW;
        int dateId = DateIds.from(ROSTER_DATE);

        IntStream.range(0, playerIds.size()).forEach(index -> {
            Long playerId = playerIds.get(index);
            String teamCode = index == 0 ? "LG" : "KT";
            jdbcTemplate.update("""
                    insert into player (player_id, player_name, player_type, position, team_code, status, team_updated_at, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, playerId, "선수" + playerId, types[index], positions[index], teamCode, "ACTIVE", now, now, now);
            jdbcTemplate.update("""
                    insert into player_profile (date_id, player_id, cost, point, avg_point, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, dateId, playerId, 1, 0, 0, now, now);
        });
    }

    private PlayerPosition rosterPosition(int index) {
        if (index < 5)
            return PlayerPosition.P;

        return List.of(
                PlayerPosition.C,
                PlayerPosition.B1,
                PlayerPosition.B2,
                PlayerPosition.B3,
                PlayerPosition.SS,
                PlayerPosition.LF,
                PlayerPosition.CF,
                PlayerPosition.RF,
                PlayerPosition.DH
        ).get(index - 5);
    }

    private int positionOrder(int index) {
        if (index < 5)
            return index + 1;

        return 1;
    }

    private void givenStartedSchedule(String homeTeamCode, String awayTeamCode) {
        given(scheduleFetchService.fetchFresh(ROSTER_DATE)).willReturn(List.of(
                scheduleGame("game-1", ROSTER_NOW.minusMinutes(10), homeTeamCode, awayTeamCode, KboGameStatus.PLAYING)
        ));
    }

    private KboScheduleGame scheduleGame(
            String gameId, LocalDateTime startAt, String homeTeamCode, String awayTeamCode, KboGameStatus status) {
        return KboScheduleGame.builder()
                .gameId(gameId)
                .startAt(startAt)
                .homeTeamCode(homeTeamCode)
                .awayTeamCode(awayTeamCode)
                .status(status)
                .build();
    }

    private PlayerRecordResponse buildPlayerRecord(long playerId) {

        return PlayerRecordResponse.builder()
                .gameId("game-1")
                .playerId(playerId)
                .name("선수" + playerId)
                .playerType(PlayerType.BATTER)
                .pos("C")
                .ab(4)
                .hit(2)
                .build();
    }

    private List<Long> defaultPlayerIds() {
        return new ArrayList<>(List.of(101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L, 110L, 111L, 112L, 113L, 114L));
    }

    private List<Long> replacementPlayerIds() {
        return new ArrayList<>(List.of(201L, 202L, 203L, 204L, 205L, 206L, 207L, 208L, 209L, 210L, 211L, 212L, 213L, 214L));
    }

}
