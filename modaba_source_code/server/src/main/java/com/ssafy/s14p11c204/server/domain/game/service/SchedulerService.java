package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.dto.MessageType;
import com.ssafy.s14p11c204.server.domain.chat.service.RedisPublisher;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final RoomMapper roomMapper;
    private final GameResultService gameResultService;
    private final GameService gameService; // 추가
    private final RedisPublisher redisPublisher;
    private final ChannelTopic channelTopic;

    // 1시간 전 알림 (기존 로직)
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    @Transactional
    public void checkUpcomingRooms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        LocalDateTime oneHourLaterPlusOneMinute = oneHourLater.plusMinutes(1);

        log.info("Scheduler running at {}. Searching rooms between {} and {}", now, oneHourLater, oneHourLaterPlusOneMinute);

        List<RoomResponseDto> rooms = roomMapper.findRoomsStartingSoon(oneHourLater, oneHourLaterPlusOneMinute);
        
        if (rooms.isEmpty()) {
            log.info("Found 0 rooms starting soon.");
        } else {
            log.info("Found {} rooms starting soon.", rooms.size());
        }

        for (RoomResponseDto room : rooms) {
            log.info("Sending notification for room {}. Type: {}", room.getRoomId(), room.getRoomType());
            
            String content = "약속 시간 1시간 전입니다! 잊지 않으셨죠?";
            MessageType type = MessageType.EVENT;

            // 스터디(FOCUS) 타입일 경우 위치 공유 요청 타입으로 변경
            if ("FOCUS".equals(room.getRoomType())) {
                content = "곧 스터디 시작 시간입니다. 위치 공유를 시작하시겠습니까?";
                type = MessageType.LOCATION_REQUEST;
            }

            ChatMessageDto message = ChatMessageDto.builder()
                    .roomId(room.getRoomId())
                    .senderId(null)
                    .senderNickname("SYSTEM")
                    .message(content)
                    .type(type)
                    .createdAt(LocalDateTime.now())
                    .build();

            redisPublisher.publish(channelTopic, message);

            // 알림 발송 완료 처리 (중복 발송 방지)
            roomMapper.updateNotiSent(room.getRoomId(), true);
        }
    }

    // 게임 시간 초과 체크 (추가됨)
    // 매 분마다 실행하여 15분 이상 지난 게임을 강제 종료 (도둑 승리)
    @Scheduled(cron = "0 * * * * *") 
    @Transactional
    public void checkTimeOverGames() {
        // 제한 시간: 15분 (테스트를 위해 짧게 설정 가능)
        int timeLimitMinutes = 15;
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(timeLimitMinutes);

        List<Long> timeOverRoomIds = roomMapper.findTimeOverRooms(timeLimit);

        for (Long roomId : timeOverRoomIds) {
            log.info("Game time over for room {}. Force finish (Thief Win).", roomId);
            
            // 현재 세션 ID 조회 (종료 처리 전)
            Long sessionId = gameService.getCurrentSessionId(roomId);
            
            // 도둑 승리로 게임 종료 처리
            gameResultService.processGameResult(roomId, "THIEF");
            
            // 알림 전송
            ChatMessageDto message = ChatMessageDto.builder()
                    .roomId(roomId)
                    .senderId(null)
                    .senderNickname("SYSTEM")
                    .message("제한 시간이 초과되었습니다. 도둑 팀 승리!")
                    .type(MessageType.GAME_END) // GAME_END 타입 사용
                    .sessionId(sessionId) // sessionId 추가
                    .build();

            redisPublisher.publish(channelTopic, message);
        }
    }
}