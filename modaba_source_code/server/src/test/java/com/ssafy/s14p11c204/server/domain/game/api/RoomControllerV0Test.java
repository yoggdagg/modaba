package com.ssafy.s14p11c204.server.domain.game.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;
import com.ssafy.s14p11c204.server.domain.game.service.RoomService;
import com.ssafy.s14p11c204.server.domain.game.RoomStatus;
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

import java.util.Collections;
import java.util.List;

import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.FRANK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomControllerV0.class)
@EnableConfigurationProperties(PathProperties.class)
class RoomControllerV0Test {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("방 조회 (GET)")
    class ReadOperations {

        @Test
        @DisplayName("내 방 목록을 조회한다")
        void findMyRooms() throws Exception {
            // Given
            RoomResponseDto room = new RoomResponseDto(1L, "My Room", "KYUNGDO", 8, 1, RoomStatus.WAITING, null, "서울", 1, "서울", 1, "Host");
            given(roomService.findMyRooms(FRANK.nickname())).willReturn(List.of(room));

            // When & Then
            mockMvc.perform(get("/api/v0/rooms/me")
                            .with(user(FRANK)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("My Room"));

            verify(roomService).findMyRooms(FRANK.nickname());
        }

        @Test
        @DisplayName("참여 가능한 방 목록을 조회한다")
        void findAvailableRooms() throws Exception {
            // Given
            given(roomService.findAvailableRooms(FRANK.nickname())).willReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/v0/rooms")
                            .with(user(FRANK)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(roomService).findAvailableRooms(FRANK.nickname());
        }

        @Test
        @DisplayName("지역으로 방을 검색한다")
        void searchRooms() throws Exception {
            // Given
            given(roomService.findRoomsByRegion("서울", "강남구", null)).willReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/v0/rooms/search")
                            .param("city", "서울")
                            .param("district", "강남구")
                            .with(user(FRANK)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(roomService).findRoomsByRegion("서울", "강남구", null);
        }
    }

    @Nested
    @DisplayName("방 생성 및 참여/퇴장 (POST, DELETE)")
    class WriteOperations {
        @Test
        @DisplayName("인증된 사용자는 새로운 방을 생성할 수 있다")
        void createRoom_Success() throws Exception {
            // Given
            RoomRequestDto requestDto = new RoomRequestDto();
            requestDto.setRoomId(1L);
            requestDto.setTitle("Test Room");
            requestDto.setMaxUser(8);
            requestDto.setPlaceName("서울");
            requestDto.setRoomType("KYUNGDO");
            requestDto.setRegionId(1);

            // When & Then
            mockMvc.perform(post("/api/v0/rooms")
                            .with(user(FRANK))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated());

            verify(roomService).createRoom(any(RoomRequestDto.class), eq(FRANK.nickname()));
        }

        @Test
        @DisplayName("인증된 사용자는 방에 참가할 수 있다")
        void joinRoom_Success() throws Exception {
            // When & Then
            long roomIdToJoin = 123L;
            mockMvc.perform(post("/api/v0/rooms/{roomId}", roomIdToJoin)
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(roomService).joinRoom(roomIdToJoin, FRANK.nickname());
        }

        @Test
        @DisplayName("인증된 사용자는 방에서 나갈 수 있다")
        void leaveRoom_Success() throws Exception {
            // When & Then
            Long roomIdToLeave = 456L;
            mockMvc.perform(delete("/api/v0/rooms/{roomId}", roomIdToLeave)
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(roomService).leaveRoom(roomIdToLeave, FRANK.nickname());
        }
    }
}
