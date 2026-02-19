package com.ssafy.s14p11c204.server.domain.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.UserLocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    public void sendLocation(String publishMessage) {
        try {
            UserLocationDto location = objectMapper.readValue(publishMessage, UserLocationDto.class);
            
            // 위치 정보를 구독한 클라이언트에게 전송
            messagingTemplate.convertAndSend("/sub/location/room/" + location.getRoomId(), location);
        } catch (Exception e) {
            log.error("LocationSubscriber Exception {}", e);
        }
    }
}