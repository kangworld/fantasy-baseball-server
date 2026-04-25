package com.kangworld.fantasybaseball.roster.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangworld.fantasybaseball.auth.token.JwtAccessTokenFactory;
import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.rediscore.lock.RedisLock;
import com.kangworld.rediscore.lock.RedisLockManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
        "delete from roster_player",
        "delete from roster",
        "delete from player_profile",
        "delete from player",
        "delete from game"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RosterControllerTest {

    private static final LocalDate ROSTER_DATE = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtAccessTokenFactory jwtAccessTokenFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisLockManager redisLockManager;

    @BeforeEach
    void setUpRedisLock() {
        given(redisLockManager.acquire(eq("roster"), eq(3L), any()))
                .willReturn(new RedisLock("fantasy-baseball::roster-1", "token"));
    }

    @Test
    void rosters_me는_accessToken이_없으면_401을_반환한다() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rosters_me는_로스터를_제출한다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("로스터 제출이 완료되었습니다."))
                .andExpect(jsonPath("$.data.date").value(ROSTER_DATE.toString()))
                .andExpect(jsonPath("$.data.players.length()").value(14))
                .andExpect(jsonPath("$.data.players[0].slotNo").value(1))
                .andExpect(jsonPath("$.data.players[0].playerId").value(101));
    }

    @Test
    void rosters_me는_제출한_로스터를_조회한다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));
        mockMvc.perform(post("/api/rosters/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // when then
        mockMvc.perform(get("/api/rosters/me")
                        .header("Authorization", "Bearer " + accessToken)
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
                        .header("Authorization", "Bearer " + accessToken)
                        .param("date", ROSTER_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
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
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rosterRequest(firstPlayerIds))));

        // when then
        mockMvc.perform(put("/api/rosters/me")
                        .header("Authorization", "Bearer " + accessToken)
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
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rosters_me는_이미_시작한_경기의_선수를_제출할_수_없다() throws Exception {
        // given
        insertRosterPlayers(defaultPlayerIds());
        insertStartedGame("LG", "HH");
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        String requestBody = objectMapper.writeValueAsString(rosterRequest(defaultPlayerIds()));

        // when then
        mockMvc.perform(post("/api/rosters/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rosters_me는_이미_시작한_경기의_선수가_같은_슬롯에_남아있으면_수정한다() throws Exception {
        // given
        List<Long> firstPlayerIds = defaultPlayerIds();
        List<Long> secondPlayerIds = replacementPlayerIds();
        secondPlayerIds.set(0, firstPlayerIds.get(0));

        insertRosterPlayers(firstPlayerIds);
        insertRosterPlayers(replacementPlayerIds());

        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        mockMvc.perform(post("/api/rosters/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rosterRequest(firstPlayerIds))));

        insertStartedGame("LG", "HH");

        // when then
        mockMvc.perform(put("/api/rosters/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rosterRequest(secondPlayerIds))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.players[0].playerId").value(101))
                .andExpect(jsonPath("$.data.players[1].playerId").value(202));
    }

    @Test
    void rosters_me는_이미_시작한_경기의_선수_슬롯이_바뀌면_거절한다() throws Exception {
        // given
        List<Long> playerIds = defaultPlayerIds();
        List<Long> movedPlayerIds = defaultPlayerIds();
        movedPlayerIds.set(0, playerIds.get(1));
        movedPlayerIds.set(1, playerIds.get(0));

        insertRosterPlayers(playerIds);

        String accessToken = jwtAccessTokenFactory.create(1L, "USER");
        mockMvc.perform(post("/api/rosters/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rosterRequest(playerIds))));

        insertStartedGame("LG", "HH");

        // when then
        mockMvc.perform(put("/api/rosters/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rosterRequest(movedPlayerIds))))
                .andExpect(status().isBadRequest());
    }

    private Map<String, Object> rosterRequest(List<Long> playerIds) {
        List<Map<String, Object>> players = new ArrayList<>();
        for (int index = 0; index < playerIds.size(); index++) {
            players.add(Map.of(
                    "slotNo", index + 1,
                    "playerId", playerIds.get(index)
            ));
        }

        return Map.of(
                "players", players
        );
    }

    private void insertRosterPlayers(List<Long> playerIds) {
        String[] positions = {"C", "B1", "B2", "B3", "SS", "LF", "CF", "RF", "DH", "P", "P", "P", "P", "P"};
        String[] types = {
                "BATTER", "BATTER", "BATTER", "BATTER", "BATTER", "BATTER", "BATTER", "BATTER", "BATTER",
                "PITCHER", "PITCHER", "PITCHER", "PITCHER", "PITCHER"
        };
        LocalDateTime now = LocalDateTime.of(2026, 5, 4, 12, 0);
        int dateId = DateIds.from(ROSTER_DATE);

        for (int index = 0; index < playerIds.size(); index++) {
            Long playerId = playerIds.get(index);
            jdbcTemplate.update("""
                    insert into player (player_id, player_name, player_type, position_code, status, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, playerId, "선수" + playerId, types[index], positions[index], "ACTIVE", now, now);
            String teamCode = index == 0 ? "LG" : "KT";
            jdbcTemplate.update("""
                    insert into player_profile (date_id, player_id, team_code, pos, cost, point, avg_point, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, dateId, playerId, teamCode, positions[index], 1, 0, 0, now, now);
        }
    }

    private void insertStartedGame(String homeTeamCode, String awayTeamCode) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                insert into game (start_at, home_team_code, away_team_code, game_status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?)
                """, now.minusMinutes(10), homeTeamCode, awayTeamCode, "PLAYING", now, now);
    }

    private List<Long> defaultPlayerIds() {
        return new ArrayList<>(List.of(101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L, 110L, 111L, 112L, 113L, 114L));
    }

    private List<Long> replacementPlayerIds() {
        return new ArrayList<>(List.of(201L, 202L, 203L, 204L, 205L, 206L, 207L, 208L, 209L, 210L, 211L, 212L, 213L, 214L));
    }

}
