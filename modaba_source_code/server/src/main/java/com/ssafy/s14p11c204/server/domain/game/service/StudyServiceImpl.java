package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.chat.service.RedisPublisher;
import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.FocusMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.GameMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.LocationMessageDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.global.util.GeometryUtil;
import com.ssafy.s14p11c204.server.domain.game.dto.StudyStateResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private final RoomMapper roomMapper;
    private final GameMapper gameMapper;
    private final FocusMapper focusMapper;
    private final GeometryUtil geometryUtil;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic channelTopic;
    private final StringRedisTemplate redisTemplate;
    private final AiReportService aiReportService;
    
    private static final String HEARTBEAT_KEY_PREFIX = "study:heartbeat:";
    private static final double ARRIVAL_DISTANCE_LIMIT = 0.0001;

    @Override
    @Transactional(readOnly = true)
    public StudyStateResponseDto getCurrentState(String nickname) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // 1. 현재 진행 중인 방이 있는지 확인
        return roomMapper.findActiveRoomIdByUserId(userId)
                .map(roomId -> {
                    // 2. 방 제목 및 세션 정보 조회
                    RoomRequestDto room = roomMapper.findRoomById(roomId).orElse(null);
                    Long sessionId = gameMapper.findCurrentSessionId(roomId).orElse(null);
                    LocalDateTime startTime = gameMapper.findCurrentSessionStartTime(roomId).orElse(null);

                    if (room == null || sessionId == null) return buildEmptyState();

                    return StudyStateResponseDto.builder()
                            .isInStudy(true)
                            .roomId(roomId)
                            .sessionId(sessionId)
                            .title(room.getTitle())
                            .startTime(startTime)
                            .build();
                })
                .orElseGet(this::buildEmptyState);
    }

    private StudyStateResponseDto buildEmptyState() {
        return StudyStateResponseDto.builder().isInStudy(false).build();
    }

    @Override
    @Transactional
    public boolean verifyArrival(Long roomId, String nickname, Double lat, Double lng) {
        // 1. 방 정보 및 목적지 좌표 조회
        RoomRequestDto room = roomMapper.findRoomById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        
        if (room.getTargetLat() == null || room.getTargetLng() == null) {
            log.warn("Study room {} has no target coordinates", roomId);
            return false;
        }

        // 2. 거리 계산
        Point targetPoint = geometryUtil.createPoint(room.getTargetLat(), room.getTargetLng());
        boolean isArrived = geometryUtil.isNear(targetPoint, lat, lng, ARRIVAL_DISTANCE_LIMIT);
        
        log.info("Arrival check for {}: target({}, {}), current({}, {}), result={}", 
                nickname, room.getTargetLat(), room.getTargetLng(), lat, lng, isArrived);

        // 3. 도착 성공 시 상태 업데이트
        if (isArrived) {
            Long userId = roomMapper.findUserIdByNickname(nickname)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
            roomMapper.updateParticipantStatus(roomId, userId, ParticipantStatus.ARRIVED);
        }
        
        return isArrived;
    }

    @Override
    @Transactional
    public Long tagUser(Long roomId, String actorNickname, String targetNickname) {
        // 1. 유저 ID 조회
        Long actorId = roomMapper.findUserIdByNickname(actorNickname)
                .orElseThrow(() -> new IllegalArgumentException("태그한 유저가 존재하지 않습니다."));
        Long targetId = roomMapper.findUserIdByNickname(targetNickname)
                .orElseThrow(() -> new IllegalArgumentException("태그된 유저가 존재하지 않습니다."));

        // 2. 현재 방에 진행 중인 세션이 있는지 확인 (없으면 생성)
        Long sessionId = gameMapper.findCurrentSessionId(roomId).orElseGet(() -> {
            log.info("Starting new study session for room {}", roomId);
            gameMapper.createGameSession(roomId);
            
            // 방 상태를 PLAYING으로 변경
            roomMapper.updateRoomStatus(roomId, "PLAYING");
            
            return gameMapper.findCurrentSessionId(roomId)
                    .orElseThrow(() -> new RuntimeException("세션 생성 실패"));
        });

        // 3. 태그 액션 로그 기록
        gameMapper.insertActionLog(sessionId, actorId, targetId, "TAG");
        log.info("Tag recorded in session {}: {} -> {}", sessionId, actorNickname, targetNickname);

        // 4. 유저들의 상태를 IN_GAME(공부 중)으로 변경
        roomMapper.updateParticipantStatus(roomId, actorId, ParticipantStatus.IN_GAME);
        roomMapper.updateParticipantStatus(roomId, targetId, ParticipantStatus.IN_GAME);

        return sessionId;
    }

    @Override
    @Transactional
    public void addFocusLog(Long sessionId, String nickname, Double score) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // 1. DB에 시계열 데이터 저장 (JSONB 형식)
        String scoreEntryJson = String.format("{\"t\": \"%s\", \"score\": %.1f}", 
                                              LocalDateTime.now(), score);
        focusMapper.upsertFocusLog(sessionId, userId, scoreEntryJson);

        // 2. 실시간 브로드캐스팅
        LocationMessageDto syncMsg = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.LOCATION_UPDATE)
                .senderNickname(nickname)
                .message(String.valueOf(score))
                .build();
        
        redisPublisher.publish(channelTopic, syncMsg);
        
        // 3. 집중도 데이터를 보냈다는 것은 살아있다는 뜻이므로 하트비트도 갱신
        heartbeat(sessionId, nickname);
    }

    @Override
    public void heartbeat(Long sessionId, String nickname) {
        String key = HEARTBEAT_KEY_PREFIX + sessionId + ":" + nickname;
        // Redis에 2분 동안 유효한 키 저장
        redisTemplate.opsForValue().set(key, "alive", 2, TimeUnit.MINUTES);
        log.debug("Heartbeat updated in Redis for {}: {}", nickname, key);
    }

    @Override
    @Transactional
    public Long endStudy(Long roomId, String nickname) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // 1. 현재 진행 중인 세션 조회
        Long sessionId = gameMapper.findCurrentSessionId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("진행 중인 스터디 세션이 없습니다."));

        // 2. 세션 종료 처리 (시간 기록)
        // updateGameSession은 avgMmr과 winnerTeam을 받는데, 스터디용으로 간단한 종료 쿼리가 필요할 수 있음.
        // 일단은 기존 메서드를 활용하되, MMR/Winner는 null 또는 더미 값 처리
        // (더 깔끔하게 하려면 GameMapper에 updateStudySessionEnd 추가 권장)
        gameMapper.updateGameSession(roomId, 0, "STUDY_END"); 

        // 3. 집중도 평균 점수 계산 및 업데이트
        focusMapper.updateAverageScore(sessionId, userId);

        // 4. 방 상태 변경 (FINISHED)
        roomMapper.updateRoomStatus(roomId, "FINISHED");
        
        // 5. Redis 하트비트 삭제
        String key = HEARTBEAT_KEY_PREFIX + sessionId + ":" + nickname;
        redisTemplate.delete(key);
        
        log.info("Study session {} ended for room {}", sessionId, roomId);

        // AI 리포트 생성 및 저장
        return aiReportService.createReport(sessionId, userId); 
    }
}