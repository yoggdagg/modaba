package com.ssafy.s14p11c204.server.domain.social.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.s14p11c204.server.domain.social.Friendship;
import com.ssafy.s14p11c204.server.domain.social.service.FriendService;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileSimpleResponse;
import com.ssafy.s14p11c204.server.global.properties.PathProperties;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

// ★ DefaultUsers (A-Z)
import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.*;
// ★ Assertions & Mockito
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FriendControllerV0.class)
@EnableConfigurationProperties(PathProperties.class)
class FriendControllerV0Test {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FriendService friendService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("조회 기능 (GET)")
    class ReadOperations {

        @Test
        @DisplayName("친구 목록 조회(ACCEPTED) 시 Grace가 반환된다")
        void showFriends() throws Exception {
            // Given
            given(friendService.showRelations(FRANK.id(), Friendship.Status.ACCEPTED))
                    .willReturn(List.of(new ProfileSimpleResponse(GRACE.id(), GRACE.nickname(), null)));

            // When
            MvcResult result = mockMvc.perform(get("/api/v0/friends")
                            .with(user(FRANK)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            verifyListContainsNickname(result, "Grace");
        }

        @Test
        @DisplayName("대기 목록 조회(PENDING) 시 Heidi가 반환된다")
        void showPending() throws Exception {
            // Given
            given(friendService.showRelations(FRANK.id(), Friendship.Status.PENDING))
                    .willReturn(List.of(new ProfileSimpleResponse(HEIDI.id(), HEIDI.nickname(), null)));

            // When
            MvcResult result = mockMvc.perform(get("/api/v0/friends/pending")
                            .with(user(FRANK)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            verifyListContainsNickname(result, "Heidi");
        }

        @Test
        @DisplayName("차단 목록 조회(REJECTED) 시 Ivan이 반환된다")
        void showBlocked() throws Exception {
            // Given
            given(friendService.showRelations(FRANK.id(), Friendship.Status.REJECTED))
                    .willReturn(List.of(new ProfileSimpleResponse(IVAN.id(), IVAN.nickname(), null)));

            // When
            MvcResult result = mockMvc.perform(get("/api/v0/friends/blocked")
                            .with(user(FRANK)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            verifyListContainsNickname(result, "Ivan");
        }
    }

    @Nested
    @DisplayName("상태 변경 기능 (POST, PATCH, DELETE)")
    class WriteOperations {

        @Test
        @DisplayName("친구 신청 (POST): Frank가 Kevin에게 요청을 보낸다")
        void sendRequest() throws Exception {
            mockMvc.perform(post("/api/v0/friends/{userId}", KEVIN.id())
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isAccepted());

            verify(friendService).sendRequest(FRANK.id(), KEVIN.id());
        }

        @Test
        @DisplayName("친구 수락 (PATCH): Frank가 Heidi의 요청을 수락한다")
        void respondRequest() throws Exception {
            mockMvc.perform(patch("/api/v0/friends/{userId}", HEIDI.id())
                            .param("status", "ACCEPTED")
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(friendService).updateRelation(FRANK.id(), HEIDI.id(), Friendship.Status.ACCEPTED);
        }

        @Test
        @DisplayName("친구 삭제 (DELETE): Grace와의 관계를 PENDING으로 되돌린다 (Soft Delete)")
        void unfriend() throws Exception {
            mockMvc.perform(delete("/api/v0/friends/{userId}", GRACE.id())
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // 컨트롤러 로직상 친구 삭제는 PENDING 상태로 업데이트함
            verify(friendService).updateRelation(FRANK.id(), GRACE.id(), Friendship.Status.PENDING);
        }

        @Test
        @DisplayName("친구 차단 (POST /block): Kevin을 차단(REJECTED)한다")
        void blockUser() throws Exception {
            mockMvc.perform(post("/api/v0/friends/block/{userId}", KEVIN.id())
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(friendService).updateRelation(FRANK.id(), KEVIN.id(), Friendship.Status.REJECTED);
        }

        @Test
        @DisplayName("차단 해제 (DELETE /block): Ivan의 차단을 해제(PENDING)한다")
        void unblockUser() throws Exception {
            mockMvc.perform(delete("/api/v0/friends/block/{userId}", IVAN.id())
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // 컨트롤러 로직상 차단 해제는 PENDING 상태로 업데이트함
            verify(friendService).updateRelation(FRANK.id(), IVAN.id(), Friendship.Status.PENDING);
        }
    }

    @Nested
    @DisplayName("예외 처리 (Unhappy Path)")
    class ExceptionHandling {

        @Test
        @DisplayName("친구 응답 시 status 파라미터가 없으면 400 Bad Request")
        void respondWithoutStatus() throws Exception {
            mockMvc.perform(patch("/api/v0/friends/{userId}", HEIDI.id())
                            .with(user(FRANK))
                            .with(csrf())) // param("status") 누락
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("음수 ID로 요청 시 400 Bad Request (@Positive 검증)")
        void invalidUserId() throws Exception {
            mockMvc.perform(post("/api/v0/friends/{userId}", -1L)
                            .with(user(FRANK))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    // --- Helper Method ---
    private void verifyListContainsNickname(MvcResult result, String expectedNickname) throws Exception {
        String content = result.getResponse().getContentAsString();
        List<ProfileSimpleResponse> actualList = objectMapper.readValue(content,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ProfileSimpleResponse.class));

        assertFalse(actualList.isEmpty(), "결과 리스트가 비어있습니다.");
        assertEquals(expectedNickname, actualList.getFirst().nickname());
    }
}