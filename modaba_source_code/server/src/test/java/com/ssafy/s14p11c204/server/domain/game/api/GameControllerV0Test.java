package com.ssafy.s14p11c204.server.domain.game.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.MyPosition;
import com.ssafy.s14p11c204.server.domain.game.dto.TagRequest;
import com.ssafy.s14p11c204.server.domain.game.service.GameResultService;
import com.ssafy.s14p11c204.server.domain.game.service.GameService;
import com.ssafy.s14p11c204.server.global.properties.PathProperties;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.FRANK;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameControllerV0.class)
@EnableConfigurationProperties(PathProperties.class)
class GameControllerV0Test {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private GameResultService gameResultService;

    private final Long roomId = 1L;

    @Nested
    @DisplayName("게임 기능 (POST, PATCH)")
    class GameFlow {

        @Test
        @DisplayName("방장은 게임을 시작할 수 있다")
        void gameStart() throws Exception {
            mockMvc.perform(post("/api/v0/games/{roomId}", roomId) // game -> games 수정
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(gameService).gameStart(roomId, FRANK.id());
        }

        @Test
        @DisplayName("참가자는 준비/준비 취소할 수 있다")
        void ready() throws Exception {
            mockMvc.perform(patch("/api/v0/games/{roomId}", roomId) // game -> games 수정
                            .param("isReady", "true")
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(gameService).ready(roomId, FRANK.id(), true);
        }

        @Test
        @DisplayName("참가자는 다른 참가자를 태그할 수 있다")
        void tag() throws Exception {
            TagRequest tagRequest = new TagRequest(roomId, "dummyPacketData");

            mockMvc.perform(patch("/api/v0/games/{roomId}/tagging", roomId) // game -> games 수정
                            .with(user(FRANK))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tagRequest)))
                    .andExpect(status().isNoContent());

            verify(gameService).tagProcess(roomId, FRANK.id(), tagRequest);
        }

        @Test
        @DisplayName("참가자는 자신의 위치를 보고할 수 있다")
        void position() throws Exception {
            MyPosition myPosition = new MyPosition(37.5, 127.5);

            mockMvc.perform(patch("/api/v0/games/{roomId}/position", roomId) // game -> games 수정
                            .with(user(FRANK))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(myPosition)))
                    .andExpect(status().isNoContent());

            verify(gameService).refreshPosition(roomId, FRANK.id(), myPosition);
        }
    }
}
