package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.dto.MessageType;
import com.ssafy.s14p11c204.server.domain.chat.service.RedisPublisher;
import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.GameMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.GameBoundaryDto;
import com.ssafy.s14p11c204.server.domain.game.dto.GameStartResponseDto;
import com.ssafy.s14p11c204.server.domain.game.dto.LocationMessageDto;
import com.ssafy.s14p11c204.server.domain.game.dto.MyPosition;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.game.dto.TagRequest;
import com.ssafy.s14p11c204.server.global.util.GeometryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
// import org.locationtech.jts.geom.Point; // 충돌 방지를 위해 제거
import com.ssafy.s14p11c204.server.domain.game.event.GameEndEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final RoomMapper roomMapper;
    private final GameMapper gameMapper;
    private final RedisPublisher redisPublisher;
    private final ActivityService activityService;
    private final ChannelTopic channelTopic;
    private final GameResultService gameResultService;
    private final RedisGeoService redisGeoService;
    private final GeometryUtil geometryUtil;
    private final ApplicationEventPublisher eventPublisher;
    
    @Value("${game.test-mode:false}")
    private boolean isTestMode;
    
    private static final int GAME_DURATION_MINUTES = 15; // 게임 제한 시간 (15분)
    private static final double UNLEASH_DISTANCE_LIMIT = 0.00005; // 탈옥 허용 거리 (약 5m)

    @Override
    @Transactional
    public void startGame(Long roomId) {
        log.info("Starting game for room {}", roomId);

        // 1. 방 상태 변경 (WAITING -> PLAYING)
        roomMapper.updateRoomStatus(roomId, "PLAYING");

        // 1-1. 게임 세션 생성 (시작 시간 기록)
        gameMapper.createGameSession(roomId);
        
        // 종료 예정 시간 계산
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(GAME_DURATION_MINUTES);

        // 2. 참여자 리스트 조회
        List<Long> participantIds = roomMapper.findParticipantIds(roomId);
        int totalCount = participantIds.size();

        // 3. 다른 방 정리 (모든 참가자에 대해 수행)
        for (Long userId : participantIds) {
            cleanupOtherRooms(userId, roomId);
        }

        // 4. 역할 배정
        int policeCount = 0;
        int thiefCount = 0;

        if (isTestMode) {
            log.info("Test mode active: assigning roles based on nickname keywords.");
            List<GameStartResponseDto.ParticipantInfo> participantsWithNick = roomMapper.findParticipantsWithNickname(roomId);
            
            for (Long userId : participantIds) {
                // 닉네임 찾기
                String nickname = participantsWithNick.stream()
                        .filter(p -> p.getUserId().equals(userId))
                        .map(GameStartResponseDto.ParticipantInfo::getNickname)
                        .findFirst()
                        .orElse("");

                PlayerRole role;
                if (nickname.contains("경찰")) {
                    role = PlayerRole.POLICE;
                    policeCount++;
                } else if (nickname.contains("도둑")) {
                    role = PlayerRole.THIEF;
                    thiefCount++;
                } else {
                    // 키워드 없으면 도둑을 기본값으로 하되, 경찰이 한 명도 없으면 마지막 유저를 경찰로
                    if (policeCount == 0 && userId.equals(participantIds.get(participantIds.size() - 1))) {
                        role = PlayerRole.POLICE;
                        policeCount++;
                    } else {
                        role = PlayerRole.THIEF;
                        thiefCount++;
                    }
                }
                roomMapper.updateParticipantRole(roomId, userId, role);
                log.info("Assigned role {} to user {} (Nickname: {})", role, userId, nickname);
            }
        } else {
            // [기존 로직] 랜덤 배정 (Shuffle)
            Collections.shuffle(participantIds);
            
            // 경찰 비율 30% (최소 1명 보장)
            policeCount = Math.max(1, (int) (totalCount * 0.3));
            thiefCount = totalCount - policeCount;

            for (int i = 0; i < totalCount; i++) {
                Long userId = participantIds.get(i);
                PlayerRole role = (i < policeCount) ? PlayerRole.POLICE : PlayerRole.THIEF;
                roomMapper.updateParticipantRole(roomId, userId, role);
            }
        }

        // 5. 참가자 상태 변경 (IN_GAME)
        roomMapper.updateAllParticipantsStatus(roomId, ParticipantStatus.IN_GAME);

        // 6. 게임 시작 알림 전송 (역할 정보 포함)
        List<GameStartResponseDto.ParticipantInfo> participants = roomMapper.findParticipantsWithNickname(roomId);
        
        // 구역 정보 조회 (전파용)
        RoomMapper.RoomBoundaryInfo boundaryInfo = roomMapper.findRoomBoundary(roomId).orElse(null);
        List<List<List<Double>>> coordinates = null;
        List<List<Double>> jailCoordinates = null;
        
        if (boundaryInfo != null) {
            coordinates = parsePolygonWkt(boundaryInfo.boundaryWkt());
            jailCoordinates = parseSingleRingWkt(boundaryInfo.jailWkt());
        }

        GameStartResponseDto gameStartDto = GameStartResponseDto.builder()
                .type("GAME_START")
                .roomId(roomId)
                .policeCount(policeCount)
                .thiefCount(thiefCount)
                .participants(participants)
                .coordinates(coordinates)
                .jailCoordinates(jailCoordinates)
                .endTime(endTime) // 종료 시간 추가
                .build();

        redisPublisher.publish(channelTopic, gameStartDto);

        // (선택) 기존 텍스트 알림도 유지할지 결정 (일단 유지)
        ChatMessageDto message = ChatMessageDto.builder()
                .roomId(roomId)
                .senderId(null)
                .senderNickname("SYSTEM")
                .message("인원이 모두 모였습니다. 게임을 시작합니다! 역할을 확인해주세요.")
                .type(MessageType.EVENT)
                .createdAt(LocalDateTime.now())
                .build();

        redisPublisher.publish(channelTopic, message);
    }

    private void cleanupOtherRooms(Long userId, Long currentRoomId) {
        List<Long> otherRoomIds = roomMapper.findOtherParticipatingRooms(userId, currentRoomId);

        for (Long otherRoomId : otherRoomIds) {
            log.info("Cleaning up user {} from other room {}", userId, otherRoomId);

            // 1. 방장 여부 확인 (Optional 적용)
            Long hostId = roomMapper.findHostIdByRoomId(otherRoomId).orElse(null);
            boolean isHost = userId.equals(hostId);

            // 2. 퇴장 처리
            roomMapper.deleteParticipant(otherRoomId, userId);

            // 3. 방장 위임 또는 방 삭제
            int remainingCount = roomMapper.countParticipants(otherRoomId);
            if (remainingCount == 0) {
                // [수정] 방 삭제 전 게임 세션 기록 먼저 삭제 (외래 키 제약 조건 해결)
                roomMapper.deleteGameSessionsByRoomId(otherRoomId);
                roomMapper.deleteRoom(otherRoomId);
                log.info("Room {} deleted (no participants left)", otherRoomId);
                
                // 방 삭제 시 관련 캐시 정리
                eventPublisher.publishEvent(new GameEndEvent(otherRoomId));
            } else if (isHost) {
                roomMapper.findOldestParticipant(otherRoomId).ifPresent(newHostId -> {
                    roomMapper.updateRoomHost(otherRoomId, newHostId);
                    log.info("Host of room {} changed to user {}", otherRoomId, newHostId);
                });
            }

            // 4. 퇴장 알림 전송 (선택 사항)
            ChatMessageDto leaveMsg = ChatMessageDto.builder()
                    .roomId(otherRoomId)
                    .senderNickname("SYSTEM")
                    .message("참가자가 다른 게임을 시작하여 퇴장했습니다.")
                    .type(MessageType.QUIT)
                    .build();
            redisPublisher.publish(channelTopic, leaveMsg);
        }
    }

    @Override
    @Transactional
    public void saveBoundary(Long roomId, GameBoundaryDto boundaryDto) {
        // 1. 게임 구역 WKT 변환 (도넛 모양 지원)
        String boundaryWkt = convertToPolygonWkt(boundaryDto.getCoordinates());
        
        // 2. 감옥 구역 WKT 변환 (단일 다각형)
        String jailWkt = convertSingleRingToWkt(boundaryDto.getJailCoordinates());
        
        log.info("Saving boundaries for room {}: Game={}, Jail={}", roomId, boundaryWkt, jailWkt);
        
        roomMapper.updateRoomBoundary(roomId, boundaryWkt, jailWkt);
    }

    // 도넛 모양(구멍 뚫린 다각형)을 위한 WKT 변환
    private String convertToPolygonWkt(List<List<List<Double>>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 구역 데이터입니다.");
        }
        
        String rings = coordinates.stream()
                .map(this::formatRing)
                .collect(Collectors.joining(", "));
        
        return "POLYGON(" + rings + ")";
    }

    // 단일 다각형(감옥 등)을 위한 WKT 변환
    private String convertSingleRingToWkt(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }
        return "POLYGON(" + formatRing(coordinates) + ")";
    }

    // 하나의 링(외곽선 또는 구멍)을 "(x y, x y, ...)" 형식으로 변환
    private String formatRing(List<List<Double>> ring) {
        if (ring.size() < 3) {
            throw new IllegalArgumentException("다각형의 링은 최소 3개의 점이 필요합니다.");
        }
        
        List<List<Double>> closedRing = new java.util.ArrayList<>(ring);
        List<Double> first = closedRing.get(0);
        List<Double> last = closedRing.get(closedRing.size() - 1);
        if (!first.equals(last)) {
            closedRing.add(first);
        }
        
        String points = closedRing.stream()
                .map(p -> p.get(0) + " " + p.get(1))
                .collect(Collectors.joining(", "));
        
        return "(" + points + ")";
    }
    
    // WKT 파싱: POLYGON((...)) -> List<List<List<Double>>>
    private List<List<List<Double>>> parsePolygonWkt(String wkt) {
        if (wkt == null || !wkt.startsWith("POLYGON")) return null;
        
        List<List<List<Double>>> polygon = new ArrayList<>();
        String content = wkt.substring(wkt.indexOf("(") + 1, wkt.lastIndexOf(")"));
        
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String ringStr = matcher.group(1);
            List<List<Double>> ring = Arrays.stream(ringStr.split(","))
                    .map(String::trim)
                    .map(point -> {
                        String[] xy = point.split("\\s+");
                        return Arrays.asList(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
                    })
                    .collect(Collectors.toList());
            polygon.add(ring);
        }
        return polygon;
    }
    
    // WKT 파싱: POLYGON((...)) -> List<List<Double>> (첫 번째 링만)
    private List<List<Double>> parseSingleRingWkt(String wkt) {
        List<List<List<Double>>> polygon = parsePolygonWkt(wkt);
        if (polygon != null && !polygon.isEmpty()) {
            return polygon.get(0);
        }
        return null;
    }

    @Override
    public void gameStart(Long roomId, Long userId) {
        log.info("Game start requested by user {} for room {}", userId, roomId);
        startGame(roomId);
    }

    @Override
    @Transactional
    public void gameStart(Long roomId, Long userId, GameBoundaryDto boundaryDto) {
        log.info("Game start with boundary requested by user {} for room {}", userId, roomId);
        saveBoundary(roomId, boundaryDto);
        startGame(roomId);
    }

    @Override
    public void ready(Long roomId, Long userId, boolean isReady) {
        log.info("User {} is ready: {} in room {}", userId, isReady, roomId);
    }

    @Override
    public void tagProcess(Long roomId, Long userId, TagRequest tagRequest) {
        log.info("Tag process requested by user {} in room {}", userId, roomId);
    }

    @Override
    public void refreshPosition(Long roomId, Long userId, MyPosition myPosition) {
        log.debug("Position refresh for user {} in room {}", userId, roomId);

        // Optional을 활용하여 현재 진행 중인 세션이 있을 때만 활동량 기록
        gameMapper.findCurrentSessionId(roomId)
                .ifPresent(sessionId ->
                        activityService.recordMovement(sessionId, userId, myPosition.latitude(), myPosition.longitude())
                );
    }

    @Override
    @Transactional(readOnly = true)
    public com.ssafy.s14p11c204.server.domain.game.dto.GameRoomDetailDto getRoomDetail(Long roomId) {
        log.info("Fetching details for room {}", roomId);

        // 1. 방 기본 정보 조회 (Optional 활용)
        return gameMapper.findGameRoomDetail(roomId)
                .map(detail -> {
                    // 2. 참여자 상세 정보 조회
                    List<com.ssafy.s14p11c204.server.domain.game.dto.GameParticipantDto> participants =
                            gameMapper.findDetailedParticipants(roomId);
                            
                    // 3. 구역 정보 조회 및 파싱 (데이터 복구용)
                    RoomMapper.RoomBoundaryInfo boundaryInfo = roomMapper.findRoomBoundary(roomId).orElse(null);
                    List<List<List<Double>>> coordinates = null;
                    List<List<Double>> jailCoordinates = null;
                    
                    if (boundaryInfo != null) {
                        coordinates = parsePolygonWkt(boundaryInfo.boundaryWkt());
                        jailCoordinates = parseSingleRingWkt(boundaryInfo.jailWkt());
                    }
                    
                    // 4. 게임 종료 예정 시간 조회 (추가됨)
                    LocalDateTime endTime = gameMapper.findCurrentSessionStartTime(roomId)
                            .map(startTime -> startTime.plusMinutes(GAME_DURATION_MINUTES))
                            .orElse(null);

                    return com.ssafy.s14p11c204.server.domain.game.dto.GameRoomDetailDto.builder()
                            .roomId(detail.getRoomId())
                            .title(detail.getTitle())
                            .status(detail.getStatus())
                            .hostId(detail.getHostId())
                            .participants(participants)
                            .coordinates(coordinates)
                            .jailCoordinates(jailCoordinates)
                            .endTime(endTime) // 종료 시간
                            .build();
                })
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다: " + roomId));
    }

    @Override
    @Transactional
    public void handleArrest(Long roomId, String thiefNickname) {
        Long thiefId = roomMapper.findUserIdByNickname(thiefNickname).orElse(null);
        if (thiefId == null) return;

        roomMapper.updateParticipantStatus(roomId, thiefId, ParticipantStatus.ARRESTED);
        log.info("User {} (ID: {}) status updated to ARRESTED in room {}", thiefNickname, thiefId, roomId);

        // 남은 도둑 수 계산
        int aliveThiefCount = countAliveThieves(roomId);
        
        checkGameEnd(roomId);
    }
    
    @Override
    @Transactional
    public void handleUnleash(Long roomId, String rescuerNickname) {
        log.info("Unleash requested by {} for room {}", rescuerNickname, roomId);
        
        // 1. 구출자 위치 조회 (Spring Data Redis Point)
        org.springframework.data.geo.Point rescuerLocation = redisGeoService.getUserLocation(roomId, rescuerNickname);
        
        if (rescuerLocation == null) {
            log.warn("Unleash failed: Rescuer {} location not found", rescuerNickname);
            sendUnleashFailMessage(roomId, rescuerNickname, "위치 정보를 찾을 수 없습니다.");
            return;
        }
        
        // 2. 감옥 구역 조회
        RoomMapper.RoomBoundaryInfo boundaryInfo = roomMapper.findRoomBoundary(roomId).orElse(null);
        if (boundaryInfo == null || boundaryInfo.jailWkt() == null) {
            log.warn("Unleash failed: Jail boundary not found for room {}", roomId);
            sendUnleashFailMessage(roomId, rescuerNickname, "감옥 구역 정보가 없습니다.");
            return;
        }
        
        Geometry jailBoundary = geometryUtil.parseWkt(boundaryInfo.jailWkt());
        
        // 3. 거리 검증 (감옥 근처 5m 이내인지)
        // Spring Point -> JTS Point 변환 없이 좌표값(Y=lat, X=lng) 직접 사용
        if (!geometryUtil.isNear(jailBoundary, rescuerLocation.getY(), rescuerLocation.getX(), UNLEASH_DISTANCE_LIMIT)) {
            log.warn("Unleash failed: Rescuer {} is too far from jail", rescuerNickname);
            sendUnleashFailMessage(roomId, rescuerNickname, "감옥과 너무 멉니다. 더 가까이 가세요!");
            return;
        }
        
        // 4. 감옥에 있는 모든 도둑 상태 변경 (ARRESTED -> IN_GAME)
        roomMapper.updateAllArrestedThievesToAlive(roomId);
        
        // 5. 남은 도둑 수 계산
        int aliveThiefCount = countAliveThieves(roomId);
        
        // 6. 탈옥 알림 전송 (공용 채널)
        LocationMessageDto unleashMsg = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.UNLEASH_RESULT)
                .roomId(roomId)
                .senderNickname(rescuerNickname)
                .message("도둑들이 탈옥했습니다! (구출자: " + rescuerNickname + ")")
                .success(true)
                .aliveThiefCount(aliveThiefCount) // 인원 수 추가
                .build();
        
        redisPublisher.publish(channelTopic, unleashMsg);
    }
    
    private void sendUnleashFailMessage(Long roomId, String nickname, String reason) {
        LocationMessageDto failMsg = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.UNLEASH_RESULT)
                .roomId(roomId)
                .senderNickname(nickname)
                .message(reason)
                .success(false)
                .build();
        
        // 실패 메시지는 개인에게만 보내는 게 좋지만, 현재 구조상 공용 채널로 보내거나
        // LocationController에서 개인 채널로 보내도록 수정해야 함.
        // 일단은 공용 채널로 보내되, 클라이언트에서 senderNickname을 보고 필터링하도록 유도.
        redisPublisher.publish(channelTopic, failMsg);
    }

    private void checkGameEnd(Long roomId) {
        int aliveThiefCount = countAliveThieves(roomId);

        if (aliveThiefCount == 0) {
            log.info("All thieves arrested in room {}. Police win!", roomId);
            endGame(roomId, "POLICE");
        }
    }
    
    // 살아있는 도둑 수 계산 (Helper)
    private int countAliveThieves(Long roomId) {
        List<Long> participantIds = roomMapper.findParticipantIds(roomId);
        int count = 0;
        for (Long userId : participantIds) {
            PlayerRole role = gameMapper.findParticipantRole(roomId, userId).orElse(null);
            ParticipantStatus status = roomMapper.findParticipantStatus(roomId, userId);
            if (role == PlayerRole.THIEF && status != ParticipantStatus.ARRESTED) {
                count++;
            }
        }
        return count;
    }
    
    // LocationController에서 호출할 수 있도록 public 메서드 추가 (인터페이스에도 추가 필요)
    public int getAliveThiefCount(Long roomId) {
        return countAliveThieves(roomId);
    }

    @Override
    @Transactional
    public void endGame(Long roomId, String winnerTeam) {
        log.info("Ending game for room {}. Winner: {}", roomId, winnerTeam);
        gameResultService.processGameResult(roomId, winnerTeam); // 주석 해제
        
        // 게임 종료 이벤트 발행 (캐시 정리 등)
        eventPublisher.publishEvent(new GameEndEvent(roomId));
    }

    @Override
    public Long getCurrentSessionId(Long roomId) {
        return gameMapper.findCurrentSessionId(roomId).orElse(null);
    }
}
