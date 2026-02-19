package com.ssafy.s14p11c204.server.domain.user.api;

import com.ssafy.s14p11c204.server.domain.user.dto.SignupRequest;
import com.ssafy.s14p11c204.server.global.util.IntegrationTestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuthIntegrationTest implements IntegrationTestUtil {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    @Test
    @DisplayName("회원가입 성공 및 중복 이메일 가입 시 409 Conflict 반환 검증")
    void signupAndDuplicateCheck() throws Exception {
        // Given
        String email = "test_" + UUID.randomUUID() + "@example.com";
        SignupRequest request = new SignupRequest(email, "TestUser", "password123!");

        // 1. First Signup - Should be 201 Created
        mockMvc.perform(post("/api/v0/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입 완료"));

        // 2. Duplicate Signup - Should be 409 Conflict
        mockMvc.perform(post("/api/v0/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                // GlobalExceptionHandler 메시지와 일치시킴
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }
}
