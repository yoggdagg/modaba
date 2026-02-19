package com.ssafy.s14p11c204.server.domain.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisGeoService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Key 형식: room:{roomId}:locations
    private String getKey(Long roomId) {
        return "room:" + roomId + ":locations";
    }

    // 유저 위치 저장 (GEOADD)
    public void saveUserLocation(Long roomId, String nickname, double lat, double lng) {
        String key = getKey(roomId);
        redisTemplate.opsForGeo().add(key, new Point(lng, lat), nickname);
        // log.debug("Saved location for {}: {}, {}", nickname, lat, lng);
    }

    // 두 유저 간 거리 계산 (GEODIST) - 미터 단위
    public Double getDistance(Long roomId, String user1, String user2) {
        String key = getKey(roomId);
        Distance distance = redisTemplate.opsForGeo().distance(key, user1, user2, RedisGeoCommands.DistanceUnit.METERS);
        
        if (distance != null) {
            return distance.getValue();
        }
        return null; // 좌표가 없거나 계산 불가
    }
    
    // 유저 위치 조회 (GEOPOS)
    public Point getUserLocation(Long roomId, String nickname) {
        String key = getKey(roomId);
        List<Point> points = redisTemplate.opsForGeo().position(key, nickname);
        
        if (points != null && !points.isEmpty()) {
            return points.get(0);
        }
        return null;
    }

    // 유저 위치 삭제 (방 나갈 때)
    public void removeUserLocation(Long roomId, String nickname) {
        String key = getKey(roomId);
        redisTemplate.opsForZSet().remove(key, nickname);
    }
}
