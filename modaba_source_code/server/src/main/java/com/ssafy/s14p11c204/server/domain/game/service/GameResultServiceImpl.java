package com.ssafy.s14p11c204.server.domain.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.dto.MessageType;
import com.ssafy.s14p11c204.server.domain.chat.service.RedisPublisher;
import com.ssafy.s14p11c204.server.domain.game.dao.GameMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.GameResultDto;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.user.dto.MmrHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameResultServiceImpl implements GameResultService {

    private final GameMapper gameMapper;
    private final RoomMapper roomMapper; // RoomDao 대신 주입
    private final RedisPublisher redisPublisher;
    private final ActivityService activityService;
    private final ChannelTopic channelTopic;
    private final ObjectMapper objectMapper; // 직렬화를 위한 Jackson Mapper

    @Override
    @Transactional
    public GameResultDto processGameResult(Long roomId, String winnerTeam) {
        log.info("Processing game result for room {}. Winner: {}", roomId, winnerTeam);

        // 0. 현재 세션 ID 미리 확보 (종료 처리 전에 가져와야 함)
        Long sessionId = gameMapper.findCurrentSessionId(roomId).orElse(null);

        // 1. 참가자 정보 조회
        List<GameResultDto.PlayerResultDto> players = gameMapper.findGameParticipants(roomId);

        // 1-1. 활동량 데이터 정산 및 저장
        if (sessionId != null) {
            for (GameResultDto.PlayerResultDto player : players) {
                activityService.finalizeActivity(sessionId, player.getUserId());
            }
        }

        // 2. 평균 MMR 계산
        int totalMmr = players.stream().mapToInt(GameResultDto.PlayerResultDto::getOldMmr).sum();
        int avgMmr = players.isEmpty() ? 1000 : totalMmr / players.size();

        // 3. 각 플레이어별 MMR 변동 계산 및 업데이트
        for (GameResultDto.PlayerResultDto player : players) {
            int changeValue = calculateMmrChange(player, winnerTeam, avgMmr);
            int newMmr = Math.max(0, player.getOldMmr() + changeValue);

            player.setNewMmr(newMmr);
            player.setChangeValue(changeValue);

            gameMapper.updateUserMmr(player.getUserId(), newMmr);

            // MMR 히스토리 저장
            if (sessionId != null) {
                MmrHistoryDto history = MmrHistoryDto.builder()
                        .userId(player.getUserId())
                        .gameId(sessionId) // sessionId 사용
                        .changeValue(changeValue)
                        .finalMmr(newMmr)
                        .reason(player.isEscaped() ? "ESCAPE_PENALTY" : "GAME_RESULT")
                        .createdAt(LocalDateTime.now())
                        .build();
                gameMapper.insertMmrHistory(history);
            }
        }

        // 4. 방 상태 변경 및 세션 업데이트
        roomMapper.updateRoomStatus(roomId, "FINISHED");
        gameMapper.updateGameSession(roomId, avgMmr, winnerTeam);

        // 5. [수정] 트랜잭션이 완전히 커밋된 후 알림 전송 (Race Condition 방지)
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendGameEndNotification(roomId, winnerTeam, sessionId);
                }
            });
        } else {
            sendGameEndNotification(roomId, winnerTeam, sessionId);
        }

        return GameResultDto.builder()
                .roomId(roomId)
                .winnerTeam(winnerTeam)
                .playerResults(players)
                .build();
    }

    private void sendGameEndNotification(Long roomId, String winnerTeam, Long sessionId) {
        try {
            String finishMessageText = "POLICE".equals(winnerTeam) ? "경찰 팀 승리!" : "도둑 팀 승리!";

            ChatMessageDto finishMessage = ChatMessageDto.builder()
                    .type(MessageType.GAME_END)
                    .roomId(roomId)
                    .senderNickname("SYSTEM")
                    .message(finishMessageText)
                    .sessionId(sessionId) // 파라미터로 받은 sessionId 사용
                    .build();

            // 객체를 JSON 문자열로 직렬화
            String jsonMessage = objectMapper.writeValueAsString(finishMessage);

            // Redis 발행 (문자열 타입으로 전송)
            redisPublisher.publish(channelTopic, jsonMessage);
            log.info("게임 종료 직렬화 알림 발송 완료: {}", jsonMessage);

        } catch (Exception e) {
            log.error("종료 알림 직렬화 중 에러 발생: {}", e.getMessage());
        }
    }

    private int calculateMmrChange(GameResultDto.PlayerResultDto player, String winnerTeam, int avgMmr) {
        if (player.isEscaped()) return -50;

        boolean isWinner = false;
        if ("POLICE".equals(winnerTeam) && player.getRole() == PlayerRole.POLICE) isWinner = true;
        if ("THIEF".equals(winnerTeam) && player.getRole() == PlayerRole.THIEF) isWinner = true;

        return isWinner ? 25 : -20; // 승리 +25, 패배 -20
    }
}
