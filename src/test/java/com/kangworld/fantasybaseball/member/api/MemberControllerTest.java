package com.kangworld.fantasybaseball.member.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = "delete from member", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signup은_회원을_생성하고_기본_정보를_반환한다() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "signup@example.com",
                  "password": "plain-password",
                  "displayName": "slugger01"
                }
                """;

        // when then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.memberId").isNumber())
                .andExpect(jsonPath("$.data.email").value("signup@example.com"))
                .andExpect(jsonPath("$.data.displayName").value("slugger01"));
    }

    @Test
    void signup은_중복_이메일이면_409를_반환한다() throws Exception {
        // given
        String firstRequestBody = """
                {
                  "email": "dup@example.com",
                  "password": "plain-password",
                  "displayName": "slugger01"
                }
                """;

        String secondRequestBody = """
                {
                  "email": "dup@example.com",
                  "password": "another-password",
                  "displayName": "slugger02"
                }
                """;

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequestBody))
                .andExpect(status().isCreated());

        // when then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRequestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void signup은_잘못된_displayName이면_400을_반환한다() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "invalid@example.com",
                  "password": "plain-password",
                  "displayName": "slugger-01"
                }
                """;

        // when then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

}
