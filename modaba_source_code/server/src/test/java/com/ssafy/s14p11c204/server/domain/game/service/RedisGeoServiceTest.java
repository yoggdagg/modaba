package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dto.UserLocationDto;
import com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RedisGeoServiceTest {

    @Autowired
    private RedisGeoService redisGeoService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Long roomId = 1L;
    private String user1 = "User1";
    private String user2 = "User2";

    @BeforeEach
    void setUp() {
        // 테스트 전 데이터 초기화
        redisTemplate.delete("room:" + roomId + ":locations");
    }

    @Test
    @DisplayName("유저 위치 저장 및 거리 계산 테스트")
    void saveAndCalculateDistance() {
        // given
        double lat1 = 35.123456;
        double lng1 = 126.123456;
        
        double lat2 = 35.123456; // 같은 위도
        double lng2 = 126.123556; // 경도만 살짝 다름 (약 9m 차이)

        // when
        redisGeoService.saveUserLocation(roomId, user1, lat1, lng1);
        redisGeoService.saveUserLocation(roomId, user2, lat2, lng2);

        // then
        Double distance = redisGeoService.getDistance(roomId, user1, user2);
        assertThat(distance).isNotNull();
        assertThat(distance).isGreaterThan(0);
        System.out.println("Distance: " + distance + "m");
    }

    @Test
    @DisplayName("유저 위치 삭제 테스트")
    void removeUserLocation() {
        // given
        redisGeoService.saveUserLocation(roomId, user1, 35.0, 126.0);

        // when
        redisGeoService.removeUserLocation(roomId, user1);

        // then
        Double distance = redisGeoService.getDistance(roomId, user1, user2); // user2는 없으므로 null이어야 함
        assertThat(distance).isNull();
    }
}