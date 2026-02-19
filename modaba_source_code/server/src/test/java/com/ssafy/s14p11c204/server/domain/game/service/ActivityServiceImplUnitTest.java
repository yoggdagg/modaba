package com.ssafy.s14p11c204.server.domain.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.s14p11c204.server.domain.ai.service.AiTestService;
import com.ssafy.s14p11c204.server.domain.game.dao.ActivityMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityServiceImplUnitTest {

    private ActivityServiceImpl activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityServiceImpl(
            Mockito.mock(RedisTemplate.class),
            new ObjectMapper(),
            Mockito.mock(ActivityMapper.class),
            Mockito.mock(AiTestService.class),
            Mockito.mock(RoomMapper.class),
            Mockito.mock(UserMapper.class)
        );
    }

    @Test
    @DisplayName("Haversine 공식을 이용한 거리 계산 검증 (강남역-역삼역)")
    void calculateDistanceTest() throws Exception {
        // Given
        double lat1 = 37.4979, lon1 = 127.0276; // 강남역
        double lat2 = 37.5012, lon2 = 127.0396; // 역삼역
        
        // Reflection을 이용해 private 메서드 호출 (테스트를 위해)
        Method method = ActivityServiceImpl.class.getDeclaredMethod("calculateDistance", double.class, double.class, double.class, double.class);
        method.setAccessible(true);

        // When
        double distance = (double) method.invoke(activityService, lat1, lon1, lat2, lon2);

        // Then
        // 강남역-역삼역 거리는 약 1,100m ~ 1,200m 사이
        assertTrue(distance > 1000 && distance < 1200, "거리는 약 1.1km 내외여야 합니다. 실제: " + distance);
    }

    @Test
    @DisplayName("좌표와 시간차를 이용한 속도 계산 검증 (km/h)")
    void calculateSpeedTest() throws Exception {
        // Given
        double lat1 = 37.4979, lon1 = 127.0276;
        double lat2 = 37.5012, lon2 = 127.0396; // 약 1,100m
        long t1 = 1700000000L;
        long t2 = 1700000060L; // 60초(1분) 차이
        
        Method method = ActivityServiceImpl.class.getDeclaredMethod("calculateSpeed", double.class, double.class, long.class, double.class, double.class, long.class);
        method.setAccessible(true);

        // When
        double speed = (double) method.invoke(activityService, lat1, lon1, t1, lat2, lon2, t2);

        // Then
        // 1.1km / 1min = 66km/h (대략적)
        assertTrue(speed > 60 && speed < 75, "시속은 약 66km/h 내외여야 합니다. 실제: " + speed);
    }
}
