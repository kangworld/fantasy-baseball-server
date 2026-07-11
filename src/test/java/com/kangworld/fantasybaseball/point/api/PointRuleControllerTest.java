package com.kangworld.fantasybaseball.point.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PointRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    void findAll은_인증없이_active_규칙을_타자와_투수로_묶어_반환한다() throws Exception {
        mockMvc.perform(get("/api/point-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.batter.length()").value(15))
                .andExpect(jsonPath("$.data.pitcher.length()").value(15))
                .andExpect(jsonPath("$.data.batter[*].recordCode", hasItem("BAT_HIT")))
                .andExpect(jsonPath("$.data.pitcher[*].recordCode", hasItem("PIT_OUT")))
                .andExpect(jsonPath("$.data.batter[0].point").isNumber())
                .andExpect(jsonPath("$.data.batter[0].description").isString())
                .andExpect(jsonPath("$.data.batter[0].playerType").doesNotExist())
                .andExpect(jsonPath("$.data.batter[0].active").doesNotExist());
    }

    @Test
    void findAll은_규칙_활성상태_변경을_다음_조회에_즉시_반영한다() throws Exception {
        mockMvc.perform(get("/api/point-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batter[*].recordCode", hasItem("BAT_HIT")));

        namedParameterJdbcTemplate.update(
                "update point_rule set active = false where record_code = :recordCode",
                Map.of("recordCode", "BAT_HIT")
        );

        mockMvc.perform(get("/api/point-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batter.length()").value(14))
                .andExpect(jsonPath("$.data.batter[*].recordCode", not(hasItem("BAT_HIT"))));
    }

}
