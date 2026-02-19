package com.ssafy.s14p11c204.server.domain.social.mapper;

import com.ssafy.s14p11c204.server.domain.social.Friendship;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileSimpleResponse;
import com.ssafy.s14p11c204.server.global.util.IntegrationTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class FriendMapperTest implements IntegrationTestUtil {

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Ensure users exist
        insertUser(EVE.id(), EVE.email(), EVE.nickname());
        insertUser(FRANK.id(), FRANK.email(), FRANK.nickname());
        insertUser(GRACE.id(), GRACE.email(), GRACE.nickname());
    }

    private void insertUser(Long id, String email, String nickname) {
        jdbcTemplate.update("INSERT INTO users (user_id, email, nickname, password, role) VALUES (?, ?, ?, 'pass1234', 'USER') ON CONFLICT (user_id) DO NOTHING",
                id, email, nickname);
    }

    @Test
    @DisplayName("upsert: 새로운 관계를 생성하거나 기존 상태를 업데이트해야 한다")
    void upsertTest() {
        long from = FRANK.id();
        long to = GRACE.id();

        // When: 초기 생성 (PENDING)
        friendMapper.upsert(from, to, Friendship.Status.PENDING);
        Friendship first = friendMapper.showRelation(from, to).orElseThrow(() -> new AssertionError("데이터가 생성되지 않았습니다."));

        // Then: 생성 확인
        assertNotNull(first);
        assertEquals(Friendship.Status.PENDING, first.status());

        // When: 상태 업데이트 (ACCEPTED)
        friendMapper.upsert(from, to, Friendship.Status.ACCEPTED);
        Friendship second = friendMapper.showRelation(from, to).orElseThrow(() -> new AssertionError("데이터가 조회되지 않습니다."));

        // Then: 업데이트 확인
        assertEquals(Friendship.Status.ACCEPTED, second.status());
    }

    @Test
    @DisplayName("findPending: 내가 차단한 유저의 신청은 목록에 나오지 않아야 한다 (필터링 검증)")
    void findPendingWithFilterTest() {
        long myId = FRANK.id();
        long blockedId = EVE.id();

        // 1. Walter가 나에게 친구 신청을 보냄
        friendMapper.upsert(blockedId, myId, Friendship.Status.PENDING);

        // 2. 내가 Walter를 차단함 (단방향 차단 가정)
        friendMapper.upsert(myId, blockedId, Friendship.Status.REJECTED);

        // When
        List<ProfileSimpleResponse> pendingList = friendMapper.findPending(myId);

        // Then
        // 차단했으므로 목록에 Mallory가 뜨면 안 됨
        assertTrue(pendingList.isEmpty(), "차단한 사용자의 신청이 목록에 노출되면 안 됩니다.");
    }

    @Test
    @DisplayName("delete: 특정 방향의 관계를 정확히 삭제해야 한다")
    void deleteTest() {
        // Given
        long from = FRANK.id();
        long to = GRACE.id();
        friendMapper.upsert(from, to, Friendship.Status.ACCEPTED);

        // When
        friendMapper.delete(from, to);
        Optional<Friendship> result = friendMapper.showRelation(from, to);

        // Then
        assertTrue(result.isEmpty(), "삭제 후 데이터는 비어있어야 합니다.");
    }

    @Test
    @DisplayName("showRelation: 존재하지 않는 관계 조회 시 null을 반환해야 한다")
    void showRelationNullTest() {
        // When
        Optional<Friendship> result = friendMapper.showRelation(12345L, 67890L);

        // Then
        assertTrue(result.isEmpty());
    }
}