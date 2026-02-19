package com.ssafy.s14p11c204.server.domain.social.service;

import com.ssafy.s14p11c204.server.domain.social.Friendship;
import com.ssafy.s14p11c204.server.domain.social.mapper.FriendMapper;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileSimpleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

// DefaultUsers Import
import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendMapper friendMapper;

    @InjectMocks
    private FriendServiceImpl friendService;

    // ==========================================
    // 🧪 Fixtures (Frank 중심)
    // ==========================================
    static class Fixtures {
        // Heidi가 Frank에게 보낸 요청 (Frank 입장에서 조회 시)
        static final Friendship HEIDI_SENT_REQUEST = Friendship.builder()
                .requesterId(HEIDI.id()) // 보낸 사람: Heidi
                .receiverId(FRANK.id())   // 받는 사람: Frank
                .status(Friendship.Status.PENDING)
                .build();
    }

    @Nested
    @DisplayName("sendRequest: 친구 신청 로직")
    class SendRequestTest {

        @Test
        @DisplayName("Case 1 (New): 새로운 친구 신청 (Frank -> Kevin)")
        void sendRequest_New() {
            // Given: Kevin -> Frank 관계 없음
            given(friendMapper.showRelation(KEVIN.id(), FRANK.id())).willReturn(Optional.empty());

            // When: Frank -> Kevin 신청
            friendService.sendRequest(FRANK.id(), KEVIN.id());

            // Then: ★ 수정됨 (delete가 아니라 upsert 확인)
            verify(friendMapper).upsert(FRANK.id(), KEVIN.id(), Friendship.Status.PENDING);

            // 검증: delete는 호출되지 않았어야 함
            verify(friendMapper, never()).delete(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Case 2 (Match): 상대(Heidi)가 이미 나에게 신청한 상태라면 -> 서로 친구(ACCEPTED)")
        void sendRequest_Match() {
            // Given: Heidi -> Frank (PENDING) 상태
            given(friendMapper.showRelation(HEIDI.id(), FRANK.id()))
                    .willReturn(Optional.of(Fixtures.HEIDI_SENT_REQUEST));

            // When: Frank도 Heidi에게 신청 (수락 의사)
            friendService.sendRequest(FRANK.id(), HEIDI.id());

            // Then: updateRelation(ACCEPTED)가 호출되어 양방향 수락 처리되어야 함
            verify(friendMapper).upsert(FRANK.id(), HEIDI.id(), Friendship.Status.ACCEPTED);
            verify(friendMapper).upsert(HEIDI.id(), FRANK.id(), Friendship.Status.ACCEPTED);
        }

        @Test
        @DisplayName("Case 3 (Existing): 이미 친구인 경우 (Frank <-> Grace)")
        void sendRequest_AlreadyFriends() {
            // Given: 이미 친구 상태
            Optional<Friendship> existing = Optional.of(Friendship.builder()
                    .status(Friendship.Status.ACCEPTED).build());
            given(friendMapper.showRelation(GRACE.id(), FRANK.id())).willReturn(existing);

            // When
            friendService.sendRequest(FRANK.id(), GRACE.id());

            // Then: 상태 유지 (혹은 중복 업데이트)
            verify(friendMapper).upsert(FRANK.id(), GRACE.id(), Friendship.Status.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("updateRelation: 관계 상태 변경")
    class UpdateRelationTest {

        @Test
        @DisplayName("ACCEPTED: 친구 수락 시 양방향 관계가 생성되어야 한다")
        void updateToAccepted() {
            // When
            friendService.updateRelation(FRANK.id(), HEIDI.id(), Friendship.Status.ACCEPTED);

            // Then: A->B, B->A 모두 ACCEPTED
            verify(friendMapper).upsert(FRANK.id(), HEIDI.id(), Friendship.Status.ACCEPTED);
            verify(friendMapper).upsert(HEIDI.id(), FRANK.id(), Friendship.Status.ACCEPTED);
        }

        @Test
        @DisplayName("PENDING: 관계를 PENDING으로 업데이트(친구 삭제/취소) 시 delete가 호출되어야 한다")
        void updateToPending() {
            // When
            // 컨트롤러에서 '친구 삭제'나 '차단 해제' 시 PENDING을 넘김
            friendService.updateRelation(FRANK.id(), GRACE.id(), Friendship.Status.PENDING);

            // Then
            verify(friendMapper).delete(FRANK.id(), GRACE.id());
        }

        @Test
        @DisplayName("REJECTED: 차단 시 내 관계는 REJECTED, 상대 관계는 삭제되어야 한다")
        void updateToRejected() {
            // When: Frank가 Eve를 차단
            friendService.updateRelation(FRANK.id(), EVE.id(), Friendship.Status.REJECTED);

            // Then
            // 1. Frank -> Eve : REJECTED (차단 기록)
            verify(friendMapper).upsert(FRANK.id(), EVE.id(), Friendship.Status.REJECTED);
            // 2. Eve -> Frank : DELETE (상대방은 나를 친구로 둘 수 없음)
            verify(friendMapper).delete(EVE.id(), FRANK.id());
        }
    }

    @Nested
    @DisplayName("showRelations: 목록 조회")
    class ShowRelationsTest {

        @Test
        @DisplayName("PENDING 조회 시 Mapper의 findPending이 호출되며, 결과 리스트가 동일해야 한다")
        void findPending() {
            // Given
            List<ProfileSimpleResponse> mockList = List.of(mock(ProfileSimpleResponse.class));
            given(friendMapper.findPending(FRANK.id())).willReturn(mockList);

            // When
            List<ProfileSimpleResponse> result =
                    friendService.showRelations(FRANK.id(), Friendship.Status.PENDING);

            // Then
            assertThat(result).isEqualTo(mockList);

            verify(friendMapper).findPending(FRANK.id());
        }

        @Test
        @DisplayName("ACCEPTED 조회 시 Mapper의 findFriends가 호출된다")
        void findFriends() {
            // When
            friendService.showRelations(FRANK.id(), Friendship.Status.ACCEPTED);

            // Then
            verify(friendMapper).findFriends(FRANK.id());
        }

        @Test
        @DisplayName("REJECTED 조회 시 Mapper의 findBlocked가 호출된다")
        void findBlocked() {
            // When
            friendService.showRelations(FRANK.id(), Friendship.Status.REJECTED);

            // Then
            verify(friendMapper).findBlocked(FRANK.id());
        }
    }
}