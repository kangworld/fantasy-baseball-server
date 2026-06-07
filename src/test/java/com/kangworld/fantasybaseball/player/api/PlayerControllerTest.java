package com.kangworld.fantasybaseball.player.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.kangworld.fantasybaseball.auth.token.JwtAccessTokenFactory;
import com.kangworld.fantasybaseball.common.date.DateIds;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
        "delete from player_profile",
        "delete from player"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlayerControllerTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 28);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtAccessTokenFactory jwtAccessTokenFactory;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUpClock() {
        given(clock.instant()).willReturn(Instant.parse("2026-04-28T03:00:00Z"));
        given(clock.getZone()).willReturn(SEOUL_ZONE_ID);
    }

    @Test
    void players는_accessToken이_없으면_401을_반환한다() throws Exception {
        // given

        // when then
        mockMvc.perform(get("/api/players"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void players는_선수풀_목록을_반환한다() throws Exception {
        // given
        insertPlayerAndProfile();
        String accessToken = jwtAccessTokenFactory.create(1L, "USER");

        // when then
        mockMvc.perform(get("/api/players")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("playerType", "BATTER")
                        .param("keyword", "김"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(76290))
                .andExpect(jsonPath("$.data[0].name").value("김현수"))
                .andExpect(jsonPath("$.data[0].type").value("BATTER"))
                .andExpect(jsonPath("$.data[0].position").value("DH"))
                .andExpect(jsonPath("$.data[0].cost").value(3))
                .andExpect(jsonPath("$.data[0].point").value(12))
                .andExpect(jsonPath("$.data[0].avgPoint").value(8))
                .andExpect(jsonPath("$.data[0].teamCode").value("LG"));
    }

    private void insertPlayerAndProfile() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        int dateId = DateIds.from(TODAY);

        jdbcTemplate.update("""
                insert into player (player_id, player_name, player_type, position, team_code, status, team_updated_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 76290L, "김현수", "BATTER", "DH", "LG", "ACTIVE", now, now, now);

        jdbcTemplate.update("""
                insert into player_profile (date_id, player_id, cost, point, avg_point, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """, dateId, 76290L, 3, 12, 8, now, now);
    }

}
