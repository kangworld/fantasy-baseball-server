package com.kangworld.fantasybaseball.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void find는_클라이언트가_이미지_경로와_조합할_asset_공개_URL을_내려준다() throws Exception {
        // when then
        mockMvc.perform(get("/api/app-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.assetPublicBaseUrl").value("https://test-player-image.s3.ap-northeast-2.amazonaws.com/public"));
    }

}
