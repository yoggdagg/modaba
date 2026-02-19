package com.ssafy.s14p11c204.server.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.s14p11c204.server.domain.chat.dto.VoiceMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RadioSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    public void handleVoiceMessage(String message) {
        try {
            log.info("[RadioSubscriber] Redis로부터 음성 수신: {}", message.substring(0, Math.min(message.length(), 100)) + "...");
            // JSON -> DTO
            VoiceMessageDto voiceDto = objectMapper.readValue(message, VoiceMessageDto.class);
            
            String destination;
            if (voiceDto.isLoopback()) {
                destination = String.format("/sub/voice/loopback/%d", voiceDto.getSenderId());
            } else {
                destination = String.format("/sub/voice/team/%d/%s", 
                                              voiceDto.getRoomId(), voiceDto.getTeam());
            }
            
            log.info("[RadioSubscriber] WebSocket 전송 시작: Destination={}, Sender={}", destination, voiceDto.getSenderId());
            
            messagingTemplate.convertAndSend(destination, voiceDto);
            
        } catch (Exception e) {
            log.error("Error processing voice message", e);
        }
    }
}
