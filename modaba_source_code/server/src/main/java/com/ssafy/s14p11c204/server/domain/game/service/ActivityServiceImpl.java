package com.ssafy.s14p11c204.server.domain.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.s14p11c204.server.domain.ai.dto.GameAiRequest;
import com.ssafy.s14p11c204.server.domain.ai.dto.GameAiResponse;
import com.ssafy.s14p11c204.server.domain.ai.service.AiTestService;
import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.ActivityMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.GameMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.ActivityRecord;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.game.dto.TrajectoryPoint;
import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ActivityMapper activityMapper;
    private final GameMapper gameMapper; // 추가
    private final AiTestService aiTestService;
    private final RoomMapper roomMapper;
    private final UserMapper userMapper;

    private static final String REDIS_KEY_PREFIX = "activity:";
    private static final long REDIS_TTL_HOURS = 2; // 게임 데이터는 2시간 후 자동 삭제

    @Override
    public void recordMovement(Long sessionId, Long userId, double lat, double lng) {
        String key = REDIS_KEY_PREFIX + sessionId + ":" + userId;
        long now = Instant.now().getEpochSecond();

        // 1. 이전 좌표 정보 가져오기 (속도 계산용) - Optional 활용
        double speed = Optional.ofNullable(redisTemplate.opsForList().range(key, -1, -1))
                .filter(list -> !list.isEmpty())
                .map(list -> objectMapper.convertValue(list.getFirst(), TrajectoryPoint.class))
                .map(last -> calculateSpeed(last.lat(), last.lng(), last.t(), lat, lng, now))
                .orElse(0.0);

        // 2. 현재 좌표 저장
        TrajectoryPoint current = new TrajectoryPoint(now, lat, lng, speed);
        redisTemplate.opsForList().rightPush(key, current);
        
        // 3. 만료 시간 설정 (메모리 관리)
        redisTemplate.expire(key, REDIS_TTL_HOURS, TimeUnit.HOURS);
    }

    @Override
    public void finalizeActivity(Long sessionId, Long userId) {
        String key = REDIS_KEY_PREFIX + sessionId + ":" + userId;
        
        // Redis 데이터를 가져옴 (없으면 빈 리스트)
        List<Object> rawData = redisTemplate.opsForList().range(key, 0, -1);
        if (rawData == null) rawData = Collections.emptyList();
        
        processAndSaveActivity(sessionId, userId, rawData, key);
    }

    private void processAndSaveActivity(Long sessionId, Long userId, List<Object> rawData, String key) {
        // 1. 시계열 데이터 가공 및 통계 계산
        List<TrajectoryPoint> trajectory = rawData.stream()
                .map(obj -> objectMapper.convertValue(obj, TrajectoryPoint.class))
                .toList();

        double totalDistance = 0.0;
        double maxSpeed = 0.0;
        double sumSpeed = 0.0;

        for (int i = 0; i < trajectory.size(); i++) {
            TrajectoryPoint current = trajectory.get(i);
            maxSpeed = Math.max(maxSpeed, current.spd());
            sumSpeed += current.spd();

            if (i > 0) {
                TrajectoryPoint prev = trajectory.get(i - 1);
                totalDistance += calculateDistance(prev.lat(), prev.lng(), current.lat(), current.lng());
            }
        }

        double avgSpeed = trajectory.isEmpty() ? 0.0 : sumSpeed / trajectory.size();
        int activityScore = (int) (totalDistance * (1.0 + avgSpeed / 10.0));

        // 2. AI 리포트 생성
        String aiReportJson = null;
        try {
            // 유저 역할 및 게임 결과 조회
            String nickname = userMapper.findById(userId).map(User::getNickname).orElse("Unknown");
            
            // [수정] sessionId로 진짜 roomId를 찾아와야 정확한 역할 조회가 가능함
            Long roomId = gameMapper.findRoomIdBySessionId(sessionId).orElse(null);
            PlayerRole role = (roomId != null) ? roomMapper.findUserRole(roomId, nickname) : PlayerRole.THIEF;
            
            ParticipantStatus status = roomMapper.findParticipantStatus(roomId != null ? roomId : sessionId, userId);
            
            // 승패 결정 (간단한 예시 로직)
            String result = (status != ParticipantStatus.ARRESTED) ? "WIN" : "LOSE";

            // 지명 정보 (현재는 수동 고정)
            List<String> locations = List.of("신창동", "수완동");

            GameAiRequest aiRequest = new GameAiRequest(
                role != null ? role.name() : "THIEF",
                result,
                (int) totalDistance,
                maxSpeed,
                (int) (trajectory.size() * 5 / 60), // 대략적인 시간 (5초 주기 가정)
                locations
            );

            GameAiResponse aiResponse = aiTestService.generateGameReport(aiRequest);
            aiReportJson = objectMapper.writeValueAsString(aiResponse);
        } catch (Exception e) {
            log.error("AI 리포트 생성 실패: {}", e.getMessage());
            // AI가 실패해도 기본 메시지를 JSON 형태로 생성하여 진행
            try {
                GameAiResponse fallbackResponse = new GameAiResponse(
                    "리포트를 생성할 수 없습니다.", 
                    "AI 서비스 일시적 오류", 
                    Collections.emptyList(), 
                    "나중에 다시 시도해주세요."
                );
                aiReportJson = objectMapper.writeValueAsString(fallbackResponse);
            } catch (Exception jsonEx) {
                aiReportJson = "{\"summary\": \"리포트 생성 실패\"}";
            }
        }

        // 3. DB 저장
        try {
            String trajectoryJson = objectMapper.writeValueAsString(trajectory);
            
            // 데이터 크기 체크 로그
            log.info("Activity data sizes - Trajectory: {} chars, AI Report: {} chars", 
                    trajectoryJson.length(), 
                    aiReportJson != null ? aiReportJson.length() : 0);

            ActivityRecord record = new ActivityRecord(
                null, sessionId, userId, trajectoryJson, 
                totalDistance, avgSpeed, maxSpeed, 
                trajectory.size(), activityScore, aiReportJson, null
            );

            activityMapper.insertActivity(record);
            redisTemplate.delete(key); 
            
        } catch (Exception e) {
            // DB 저장 실패가 전체 게임 종료 로직을 망치지 않도록 예외를 잡아서 로그만 남김
            log.error("CRITICAL: Failed to save activity record to DB for session {}, user {}. Error: {}", 
                    sessionId, userId, e.getMessage(), e);
        }
    }

    @Override
    public List<ActivityRecord> getUserActivities(Long userId) {
        return activityMapper.findByUserId(userId);
    }

    @Override
    public ActivityRecord getActivityDetail(Long sessionId, Long userId) {
        return activityMapper.findBySessionAndUser(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게임의 활동 기록을 찾을 수 없습니다. Session: " + sessionId));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3; // 지구 반지름 (m)
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                   Math.cos(phi1) * Math.cos(phi2) *
                   Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private double calculateSpeed(double lat1, double lon1, long t1, double lat2, double lon2, long t2) {
        if (t1 == t2) return 0.0;
        double distanceMeters = calculateDistance(lat1, lon1, lat2, lon2);
        double timeSeconds = t2 - t1;
        
        return (distanceMeters / timeSeconds) * 3.6;
    }
}