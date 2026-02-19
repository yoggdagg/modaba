package com.ssafy.s14p11c204.server.domain.game.api;

import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.LocationMessageDto;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.game.service.ActivityService;
import com.ssafy.s14p11c204.server.domain.game.service.GameService;
import com.ssafy.s14p11c204.server.domain.game.service.RedisGeoService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.global.util.GeometryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import com.ssafy.s14p11c204.server.domain.game.event.GameEndEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisGeoService redisGeoService;
    private final GameService gameService;
    private final ActivityService activityService;
    private final RoomMapper roomMapper;
    private final GeometryUtil geometryUtil;
    
    // 테스트를 위해 50.0m로 유지
    private static final double ARREST_DISTANCE_LIMIT = 50.0;
    
    // 구역 이탈 허용 오차 (약 5m)
    private static final double BOUNDARY_BUFFER = 0.00005; 
    
    // 경고 메시지 쿨타임 (밀리초) - 5초
    private static final long WARNING_COOL_DOWN_MS = 5000;

    // 구역 정보 캐싱 (roomId -> Geometry)
    private final Map<Long, Geometry> roomBoundaries = new ConcurrentHashMap<>();
    
    // 경고 메시지 마지막 전송 시간 캐싱 (roomId -> (nickname -> timestamp))
    private final Map<Long, Map<String, Long>> lastWarningTimes = new ConcurrentHashMap<>();

    @EventListener
    public void handleGameEndEvent(GameEndEvent event) {
        Long roomId = event.getRoomId();
        roomBoundaries.remove(roomId);
        lastWarningTimes.remove(roomId);
        log.info("Cleared location cache for room {}", roomId);
    }

    @MessageMapping("/room/{roomId}/location")
    public void handleLocation(@DestinationVariable Long roomId, 
                               @Payload LocationMessageDto message,
                               Principal principal) {
        
        // [디버깅용 로그] 메서드 진입 확인
        log.info("LocationController received message for room {}: {}", roomId, message);

        String senderNickname = message.getSenderNickname();
        String senderEmail = null;
        Long userId = null;
        
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            CurrentUser currentUser = (CurrentUser) auth.getPrincipal();
            senderNickname = currentUser.nickname();
            senderEmail = currentUser.getUsername();
            userId = currentUser.id();
            log.info("Authenticated user: {}, Nickname: {}", senderEmail, senderNickname);
        } else {
            log.warn("Unauthenticated principal: {}", principal);
        }

        message.setSenderNickname(senderNickname);
        message.setRoomId(roomId);

        // [수정] 모든 메시지 타입에 대해 역할 정보 보완 (방어 코드)
        if (message.getRole() == null && senderNickname != null) {
            PlayerRole role = roomMapper.findUserRole(roomId, senderNickname);
            message.setRole(role);
            // log.debug("Role recovered for {}: {}", senderNickname, role);
        }

        if (message.getType() == LocationMessageDto.MessageType.LOCATION) {
            redisGeoService.saveUserLocation(roomId, senderNickname, message.getLat(), message.getLng());
            
            // 활동량 기록 추가
            if (userId != null) {
                activityService.recordMovement(roomId, userId, message.getLat(), message.getLng());
            }

            // PlayerRole senderRole = roomMapper.findUserRole(roomId, senderNickname); // 위에서 처리했으므로 제거
            // message.setRole(senderRole);
            message.setType(LocationMessageDto.MessageType.LOCATION_UPDATE);

            // 구역 이탈 체크
            checkOutOfBound(roomId, senderNickname, senderEmail, message.getLat(), message.getLng());

            broadcastLocation(roomId, message, senderEmail);
            
        } else if (message.getType() == LocationMessageDto.MessageType.ARREST_REQUEST) {
            handleArrestRequest(roomId, senderNickname, message);
        } else if (message.getType() == LocationMessageDto.MessageType.UNLEASH_REQUEST) {
            // 탈옥 요청 처리
            gameService.handleUnleash(roomId, senderNickname);
        }
    }
    
    private void checkOutOfBound(Long roomId, String nickname, String email, Double lat, Double lng) {
        if (lat == null || lng == null) return;
        
        // 1. 구역 정보 가져오기 (캐시 확인 -> DB 조회)
        Geometry boundary = roomBoundaries.computeIfAbsent(roomId, id -> {
            return roomMapper.findRoomBoundary(id)
                    .map(info -> geometryUtil.parseWkt(info.boundaryWkt()))
                    .orElse(null);
        });
        
        if (boundary == null) {
            log.warn("Boundary check skipped: No boundary info for room {}", roomId);
            return; 
        }
        
        // 2. 잡힌 상태인지 확인 (잡힌 사람은 이탈 체크 제외)
        Long userId = roomMapper.findUserIdByNickname(nickname).orElse(null);
        if (userId != null) {
            ParticipantStatus status = roomMapper.findParticipantStatus(roomId, userId);
            if (status == ParticipantStatus.ARRESTED) {
                log.debug("Boundary check skipped: User {} is ARRESTED", nickname);
                return;
            }
        }

        // 3. 이탈 여부 확인
        boolean isOut = geometryUtil.isOutOfBound(boundary, lat, lng, BOUNDARY_BUFFER);
        log.info("Boundary check for {}: lat={}, lng={}, isOut={}", nickname, lat, lng, isOut);

        if (isOut) {
            // 4. 쿨타임 체크
            Map<String, Long> roomWarnings = lastWarningTimes.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
            long lastTime = roomWarnings.getOrDefault(nickname, 0L);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastTime < WARNING_COOL_DOWN_MS) {
                log.debug("Warning suppressed for {}: Cool-down active", nickname);
                return;
            }
            
            log.warn("User {} is out of bound in room {}", nickname, roomId);
            
            // 5. 경고 메시지 전송 (개인에게만)
            LocationMessageDto warningMsg = LocationMessageDto.builder()
                    .type(LocationMessageDto.MessageType.WARNING)
                    .roomId(roomId)
                    .message("구역을 벗어났습니다! 10초 내로 복귀하세요.")
                    .build();
            
            if (email != null) {
                messagingTemplate.convertAndSendToUser(email, "/sub/game/" + roomId + "/location", warningMsg);
                log.info("Sent WARNING message to {}", email);
                roomWarnings.put(nickname, currentTime); // 마지막 전송 시간 업데이트
            }
        }
    }

    private void broadcastLocation(Long roomId, LocationMessageDto message, String senderEmail) {
        // [수정] 역할 정보가 없으면 DB에서 조회하여 보완 (방어 코드)
        if (message.getRole() == null) {
            PlayerRole role = roomMapper.findUserRole(roomId, message.getSenderNickname());
            message.setRole(role);
            log.warn("Role missing in message from {}, recovered from DB: {}", message.getSenderNickname(), role);
        }

        // [변경] 개별 전송 루프를 제거하고 공용 채널로 한 번에 전송 (Broadcast)
        // 안드로이드 팀은 /sub/game/{roomId}/location 채널 하나만 구독하면 됩니다.
        String destination = "/sub/game/" + roomId + "/location";
        messagingTemplate.convertAndSend(destination, message);
        
        log.info("Broadcasted location from {} to room {}", message.getSenderNickname(), roomId);
    }

    private void handleArrestRequest(Long roomId, String policeNickname, LocationMessageDto request) {
        String thiefNickname = request.getTargetNickname();
        Double distance = redisGeoService.getDistance(roomId, policeNickname, thiefNickname);
        
        log.info("Arrest request: Police={}, Thief={}, Distance={}", policeNickname, thiefNickname, distance);
        
        // 현재 세션 ID 조회
        Long sessionId = gameService.getCurrentSessionId(roomId);

        LocationMessageDto result = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.ARREST_RESULT)
                .roomId(roomId)
                .senderNickname(policeNickname)
                .targetNickname(thiefNickname)
                .role(PlayerRole.POLICE) // 검거 결과는 경찰이 주체이므로 POLICE 설정
                .sessionId(sessionId)
                .build();

        if (distance != null && distance <= ARREST_DISTANCE_LIMIT) {
            result.setSuccess(true);
            result.setMessage(thiefNickname + "님을 검거했습니다!");
            gameService.handleArrest(roomId, thiefNickname);
            
            // 남은 도둑 수 조회 및 설정
            int aliveThiefCount = gameService.getAliveThiefCount(roomId);
            result.setAliveThiefCount(aliveThiefCount);
            
            log.info("Arrest SUCCESS: {} caught {}. Alive thieves: {}", policeNickname, thiefNickname, aliveThiefCount);
            
        } else {
            result.setSuccess(false);
            result.setMessage("거리가 너무 멉니다. (현재 거리: " + (distance != null ? String.format("%.2f", distance) : "알 수 없음") + "m)");
            log.info("Arrest FAILED: Distance too far or unknown.");
        }

        // [변경] 개별 전송 루프를 제거하고 공용 채널로 한 번에 전송 (Broadcast)
        String destination = "/sub/game/" + roomId + "/location";
        messagingTemplate.convertAndSend(destination, result);
        
        log.info("Broadcasted arrest result for room {}", roomId);
    }
}