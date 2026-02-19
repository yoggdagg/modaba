package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dao.ActivityMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.ActivityRecord;
import com.ssafy.s14p11c204.server.domain.game.dto.TrajectoryPoint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration.class)
class ActivityServiceIntegrationTest {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Long TEST_SESSION_ID = 9999L;
    private final Long TEST_USER_ID = 777L;
    private final Long TEST_ROOM_ID = 555L;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // 1. 테스트용 지역 생성 (이미 있을 수 있으므로 무시)
        jdbcTemplate.execute("INSERT INTO regions (region_id, city, district, neighborhood) VALUES (960, '광주', '북구', '용봉') ON CONFLICT DO NOTHING");
        
        // 2. 테스트용 유저 생성
        jdbcTemplate.execute("INSERT INTO users (user_id, nickname, email, password) VALUES (" + TEST_USER_ID + ", '활동유저', 'act@test.com', 'pass') ON CONFLICT DO NOTHING");
        
        // 3. 테스트용 방 생성
        jdbcTemplate.execute("INSERT INTO rooms (room_id, host_id, type, title, region_id) VALUES (" + TEST_ROOM_ID + ", " + TEST_USER_ID + ", 'KYUNGDO', '테스트방', 960) ON CONFLICT DO NOTHING");
        
        // 4. 테스트용 게임 세션 생성 (이게 있어야 활동 기록 저장이 됨!)
        jdbcTemplate.execute("INSERT INTO game_sessions (session_id, room_id, winner_team) VALUES (" + TEST_SESSION_ID + ", " + TEST_ROOM_ID + ", 'POLICE') ON CONFLICT DO NOTHING");
    }

    @AfterEach
    void tearDown() {
        // Redis 데이터 청소
        String key = "activity:" + TEST_SESSION_ID + ":" + TEST_USER_ID;
        redisTemplate.delete(key);
        
        // DB 데이터 청소 (자식부터 부모 순으로 삭제)
        jdbcTemplate.execute("DELETE FROM user_game_activities WHERE session_id = " + TEST_SESSION_ID);
        jdbcTemplate.execute("DELETE FROM game_sessions WHERE session_id = " + TEST_SESSION_ID);
        jdbcTemplate.execute("DELETE FROM rooms WHERE room_id = " + TEST_ROOM_ID);
        jdbcTemplate.execute("DELETE FROM users WHERE user_id = " + TEST_USER_ID);
    }

    @Test
    @DisplayName("이동 기록부터 DB 저장 및 조회까지의 전체 파이프라인 검증")
    void fullActivityPipelineTest() throws Exception {
        // [Step 1] 이동 기록 (C - Redis)
        // 1. 강남역 (37.4979, 127.0276)
        activityService.recordMovement(TEST_SESSION_ID, TEST_USER_ID, 37.4979, 127.0276);
        Thread.sleep(1100); // 1초 대기 (속도 계산용)
        
        // 2. 역삼역 (37.5012, 127.0396) - 약 1km 거리
        activityService.recordMovement(TEST_SESSION_ID, TEST_USER_ID, 37.5012, 127.0396);

        // [Step 2] 활동 종료 및 DB 저장 (C - Postgres)
        activityService.finalizeActivity(TEST_SESSION_ID, TEST_USER_ID);

        // [Step 3] 내 활동 리스트 조회 (R)
        List<ActivityRecord> activities = activityService.getUserActivities(TEST_USER_ID);
        assertFalse(activities.isEmpty(), "저장된 활동 내역이 있어야 합니다.");
        
        ActivityRecord summary = activities.get(0);
        assertEquals(TEST_SESSION_ID, summary.sessionId());
        assertTrue(summary.totalDistance() > 1000, "이동 거리가 1km 이상이어야 합니다. 실제: " + summary.totalDistance());
        assertNotNull(summary.createdAt());

        // [Step 4] 상세 활동(궤적) 조회 (서비스가 직접 해체해서 반환함)
        ActivityRecord detail = activityService.getActivityDetail(TEST_SESSION_ID, TEST_USER_ID);
        assertNotNull(detail, "상세 내역은 null일 수 없습니다.");

        // [Step 5] JSONB 궤적 데이터 역직렬화 및 검증 (record 매핑 확인)
        List<TrajectoryPoint> points = objectMapper.readValue(detail.trajectory(), new TypeReference<>() {});
        assertEquals(2, points.size(), "궤적 포인트는 2개여야 합니다.");
        
        TrajectoryPoint firstPoint = points.get(0);
        assertEquals(37.4979, firstPoint.lat());
        assertEquals(127.0276, firstPoint.lng());
        
        // 속도가 계산되었는지 확인 (첫 포인트는 0, 두번째 포인트는 양수)
        assertTrue(points.get(1).spd() > 0, "두 번째 포인트의 속도는 0보다 커야 합니다.");

        // [Step 6] Redis 데이터 삭제 확인
        String key = "activity:" + TEST_SESSION_ID + ":" + TEST_USER_ID;
        Boolean hasKey = redisTemplate.hasKey(key);
        assertFalse(hasKey != null && hasKey, "저장 후 Redis 데이터는 삭제되어야 합니다.");
    }

    @Test
    @DisplayName("데이터가 없는 상태에서 조회 시도시 예외가 발생해야 함")
    void getActivityDetail_WithNoData_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> 
            activityService.getActivityDetail(TEST_SESSION_ID, 888L)
        );
    }
}
