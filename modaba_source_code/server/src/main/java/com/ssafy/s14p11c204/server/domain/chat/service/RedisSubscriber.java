package com.ssafy.s14p11c204.server.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    public void sendMessage(String publishMessage) {
        try {
            log.info("RedisSubscriber received message: {}", publishMessage);

            String jsonString = publishMessage;
            // 이중 인코딩 처리
            if (publishMessage.startsWith("\"") && publishMessage.endsWith("\"")) {
                jsonString = objectMapper.readValue(publishMessage, String.class);
            }

            // 1. JsonNode로 파싱 (필드 확인용)
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            if (!jsonNode.has("roomId")) {
                log.error("메시지에 roomId가 없습니다! 전송을 중단합니다: {}", jsonString);
                return;
            }
            Long roomId = jsonNode.get("roomId").asLong();

            // 2. Map으로 변환 (전송용 - 메타데이터 노출 방지 및 필드 보존)
            Map<String, Object> payload = objectMapper.convertValue(jsonNode, Map.class);

            // 3. 전송 로직
            if (jsonNode.has("recipientId") && !jsonNode.get("recipientId").isNull()) {
                String recipientId = jsonNode.get("recipientId").asText();
                String userDestination = "/sub/chat/user/" + recipientId;
                log.info("RedisSubscriber sending private message to: {}", userDestination);
                messagingTemplate.convertAndSend(userDestination, (Object) payload);
            } 
            else if (jsonNode.has("targetRole") && !jsonNode.get("targetRole").isNull()) {
                String targetRole = jsonNode.get("targetRole").asText();
                String teamDestination;
                if ("POLICE".equals(targetRole)) {
                    teamDestination = "/sub/chat/room/" + roomId + "/police";
                } else {
                    teamDestination = "/sub/chat/room/" + roomId + "/thief";
                }
                log.info("RedisSubscriber sending team message to: {}", teamDestination);
                messagingTemplate.convertAndSend(teamDestination, (Object) payload);
            } 
            else {
                String roomDestination = "/sub/chat/room/" + roomId;
                log.info("RedisSubscriber sending to room destination: {}", roomDestination);
                messagingTemplate.convertAndSend(roomDestination, (Object) payload);
            }

        } catch (Exception e) {
            log.error("Exception in RedisSubscriber: {}", e.getMessage(), e);
        }
    }
}
